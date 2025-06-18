/**
 * Google Play Store crawler.
 * 
 * This script collects seed apps from the Google Play Store, performs a breadth-first
 * crawl of related apps up to a fixed depth, collects metadata, and stores everything
 * in a SQLite database. A filtered list of app package names is written to a CSV file.
 * 
 * Usage: Run with Node.js or in Docker. See README for details.
 * 
 * @author Philipp Beer
 */

import { Database } from './db.js';
import gplay from 'google-play-scraper';
import { createLogger } from './logger.js';
import cliProgress from 'cli-progress';
import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';
import fs from 'fs';

// Parse the command line arguments
const argv = yargs(hideBin(process.argv))
  .option('country', {
    alias: 'c',
    type: 'string',
    description: 'Country code for crawling',
    default: 'at'
  })
  .option('output', {
    alias: 'o',
    type: 'string',
    description: 'Output directory',
    demandOption: true
  })
  .option('onlyFree', {
    alias: 'f',
    type: 'boolean',
    description: 'Only save free apps',
    default: false
  })
  .option('onlyMinInstalls', {
    alias: 'i',
    type: 'number',
    description: 'Only save apps with at least this many installs',
    default: 0
  })
  .option('max', {
    alias: 'm',
    type: 'number',
    description: 'Crawls up to this many apps',
    default: 999999999
  })
  .option('log', {
    alias: 'l',
    type: 'string',
    description: 'Log file',
  })
  .help()
  .argv;

const progressBar = new cliProgress.SingleBar({}, cliProgress.Presets.shades_classic);
const logger = createLogger(argv.log);
const db = new Database(`${argv.output}/apps.db`, logger);
const categories = gplay.category;
const collections = gplay.collection;
const maxCallsPerSecond = 25; // Maximum number of calls to the Google Play Store API per second

let appsCrawled = 0;

/**
 * Entrypoint for the crawler script.
 * Creates the database schema, collects seed apps,
 * discovers related apps, queries metadata, and writes results to disk.
 */
(async () => {
  logger.info("Starting crawl ...");
  await db.createTables();
  
  logger.info("Retrieving seed apps ...");
  await getSeedApps();
  logger.info("Retrieved seed apps");

  await getRelatedApps();
  logger.info("Got related apps");
  
  logger.info("Retrieving app info ...");
  const apps = await db.getAppsNotInInfo();
  const packageNames = apps.map(app => app.package);
  await queryAppInfo(packageNames);
  logger.info("Retrieved app info");
  
  logger.info("Saving to CSV ...");
  await saveToCSV();
  logger.info("Saved to CSV");
})();

/**
 * Retrieves the top 200 apps for each category and collection from the Play Store.
 * Saves all resulting apps to the database as seed apps.
 * 
 * @returns {Promise<void>}
 */
async function getSeedApps() {
  for (let category in categories) {
    for (let collection in collections) {
      try {
        let result = await gplay.list({
          category: categories[category],
          collection: collections[collection],
          num: 200,
          lang: "en",
          country: argv.country,
          fullDetail: false,
          throttle: 10 // 10 requests per second
        });
        if (appsCrawled > argv.max) {
          result = result.slice(0, argv.max);
          let amountInserted = await db.savePackages(result);
          appsCrawled += amountInserted;
          return;
        }
        let amountInserted = await db.savePackages(result);
        appsCrawled += amountInserted;
      } catch (error) {
        logger.error(`Failed to fetch ${error} `);
      }
    };
  }
}

/**
 * Recursively retrieves related apps from the Play Store using a BFS traversal.
 * Related apps are inserted into the database and visited up to the given depth.
 * 
 * @param {number} [maxDepth=9] - Maximum BFS depth to follow.
 * @returns {Promise<void>}
 */
async function getRelatedApps(maxDepth = 9) {
  let currentDepth = 0;

  while (currentDepth < maxDepth) {
    if (appsCrawled >= argv.max) {
      logger.info(`Already crawled ${appsCrawled} apps, stopping ...`);
      break;
    }
    const appsAtCurrentDepth = await db.getAppsByDepthAndFlag(currentDepth, 0); // A value of '0' means that the app has not been queried for similar apps yet.
    
    progressBar.start(appsAtCurrentDepth.length, 0);
    
    console.log(`Apps at depth ${currentDepth}: ${appsAtCurrentDepth.length}`);
    if (appsAtCurrentDepth.length === 0) {
      currentDepth++;
      continue; // No more apps to process at this depth
    }
    
    const queue = appsAtCurrentDepth.map(app => async () => {
      try {
        if (appsCrawled >= argv.max) {
          logger.info(`Already crawled ${appsCrawled} apps, stopping ...`);
          return;
        }
        const similarApps = await fetchSimilarApps(app.package);
        if (similarApps == null) {
          // There was an error fetching similar apps
          await db.updateAppError(app, currentDepth, -2); // Errors are marked with the value '-2'
        } else {
          for (const similarApp of similarApps) {
            const exists = await db.checkIfAppExists(similarApp.appId);
            if (!exists) {
              try {
                await db.insertApp(similarApp, currentDepth + 1, false);
                appsCrawled++;
              } catch (error) {
                logger.error(`Failed to insert app ${similarApp.appId} with depth ${currentDepth + 1} ${error}`);
              }
            }
          }
          await db.updateSimilarQueried(app.package, true);
        }
        progressBar.increment();
    } catch (error) {
      logger.error(`Unhandled error while processing ${app.package} ${error}`);
    }
    })
    for (let i = 0; i < queue.length; i += maxCallsPerSecond) {
      if (appsCrawled < argv.max) { 
        await Promise.all(queue.slice(i, i + maxCallsPerSecond).map(fn => fn()));
        await new Promise(resolve => setTimeout(resolve, 700));
      }
      
    }
    progressBar.stop();
    currentDepth++;
  }
}

/**
 * Fetches similar apps for a given Play Store package ID.
 * 
 * @param {string} packageId - The Play Store package name.
 * @returns {Promise<Array<Object>|null>} - Array of similar apps, or null on failure.
 */
async function fetchSimilarApps(packageId) {
  try {
    const result = await gplay.similar({
      appId: packageId,
      lang: "en",
      country: "at",
      fullDetail: false,
      throttle: 10
    });
    return result;
  } catch (error) {
    logger.error(`Failed to fetch similar apps for ${packageId} ${error}`);
    return null;
  }
}

/**
 * Queries metadata for a list of Play Store packages and stores the result in the database.
 * 
 * @param {Array<string>} packageNames - List of package names to query.
 * @returns {Promise<void>}
 */
async function queryAppInfo(packageNames) {
  logger.info(`Querying ${packageNames.length} apps ...`);
  progressBar.start(packageNames.length, 0);

  const pendingTasks = [];
  let completed = 0;

  const queue = packageNames.map(packageName => async () => {
    try {
      const result = await gplay.app({
        appId: packageName,
        lang: 'en',
        country: 'at'
      });
      await save(packageName, result);
    } catch (error) {
      logger.error(`Failed to fetch ${packageName}: ${error}`);
    } finally {
      completed++;
      progressBar.update(completed);
    }
  });

  const processQueue = () => {
    const tasksToProcess = queue.splice(0, maxCallsPerSecond);
    for (const task of tasksToProcess) {
      pendingTasks.push(task());
    }
  };

  const intervalId = setInterval(processQueue, 1000);

  // Wait until queue is empty and all tasks are done
  while (queue.length > 0) {
    await new Promise(resolve => setTimeout(resolve, 100));
  }

  clearInterval(intervalId);

  // Wait for all started tasks to finish
  await Promise.all(pendingTasks);
}

/**
 * Saves metadata for a given app into the database.
 * 
 * @param {string} packageName - The package name of the app.
 * @param {Object} result - Metadata returned from `google-play-scraper`.
 * @returns {Promise<void>}
 */
function save(packageName, result) {
  let installs = result.installs;
  let minInstalls = result.minInstalls;
  let maxInstalls = result.maxInstalls;
  let free = result.free;
  let json = JSON.stringify(result);
  return db.savePackageInfo(packageName, installs, minInstalls, maxInstalls, free, json)
}

/**
 * Save a list of free apps with minimum install count to CSV.
 * Output path is determined by `argv.output`.
 * 
 * @returns {Promise<void>}
 */
async function saveToCSV() {
  const apps = await db.getAppsAtLeast(argv.onlyFree, argv.onlyMinInstalls);
  logger.info(`Found ${apps.length} apps with at least ${argv.onlyMinInstalls} installs`);
  const pids = apps.map(app => app.package);
  fs.writeFileSync(`${argv.output}/apps.csv`, pids.join('\n'));
  logger.info(`Saved ${pids.length} apps to ${argv.output}/apps-free.csv`);
}