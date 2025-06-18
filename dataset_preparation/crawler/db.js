/**
 * SQLite database handler for the Google Play Store crawler.
 * 
 * This module defines a `Database` class that manages all persistent storage operations
 * for the crawler. It supports creating tables, inserting and querying app metadata,
 * tracking crawl depth and status, and exporting filtered results.
 * 
 * Usage: Instantiated by the crawler with a target `.db` file and a logger instance.
 * 
 * @module db.js
 * @author Philipp Beer
 */

import sqlite3 from 'sqlite3';


/**
 * Class for managing SQLite database operations related to Play Store apps.
 */
export class Database {

    /**
     * Creates a new Database instance.
     * @param {string} dbName - Path to the SQLite database file.
     * @param {Object} logger - Logger instance for logging messages.
     */
    constructor(dbName, logger) {
        this.logger = logger;
        this.db = new sqlite3.Database(dbName, (err) => {
            if (err) {
                this.logger.error('Error connecting to the SQLite database:', err.message);
            }
        });
    }

    /**
     * Creates the `pids_table` and `info_table` tables if they do not exist.
     * @returns {Promise<void>}
     */
    createTables() {
        return new Promise((resolve, reject) => {
            this.db.serialize(() => {
                this.db.run(`CREATE TABLE IF NOT EXISTS pids_table (
                    package TEXT PRIMARY KEY,
                    downloads INTEGER,
                    json TEXT,
                    depth INTEGER DEFAULT 0,
                    similar_queried INTEGER DEFAULT 0
                )`, (err) => {
                    if (err) {
                        this.logger.error('Error creating pids_table:', err.message);
                        return reject(err);
                    }
                });

                this.db.run(`CREATE TABLE IF NOT EXISTS info_table (
                    package TEXT PRIMARY KEY,
                    installs TEXT,
                    minInstalls INTEGER,
                    maxInstalls INTEGER,
                    free BOOLEAN,
                    json TEXT
                )`, (err) => {
                    if (err) {
                        this.logger.error('Error creating info_table:', err.message);
                        return reject(err);
                    }
                    resolve();
                });
            });
        });
    }

    /**
     * Saves app metadata into the `info_table`.
     * @param {string} packageName - The app's package name.
     * @param {string} installs - Raw install string.
     * @param {number} minInstalls - Minimum install estimate.
     * @param {number} maxInstalls - Maximum install estimate.
     * @param {boolean} free - Whether the app is free.
     * @param {string} json - Raw JSON-encoded metadata.
     * @returns {Promise<void>}
     */
    savePackageInfo(packageName, installs, minInstalls, maxInstalls, free, json) {
        return new Promise((resolve, reject) => {
            this.db.run(`INSERT INTO info_table (package, installs, minInstalls, maxInstalls, free, json) VALUES (?, ?, ?, ?, ?, ?)`,
                [packageName, installs, minInstalls, maxInstalls, free, json], (err) => {
                    if (err) {
                        this.logger.error(`Failed to save package info ${packageName} to the database:`, err.message);
                        reject(err);
                    } else {
                        this.logger.debug(`Saved package info for ${packageName}`);
                        resolve();
                    }
                });
        });
    }

    getAppsNotInInfo() {
        // get those apps which are in pid but are not in info
        return new Promise((resolve, reject) => {
            this.db.all(`SELECT * FROM pids_table WHERE package NOT IN (SELECT package FROM info_table)`, (err, rows) => {
                if (err) {
                    this.logger.error('Error fetching apps:', err.message);
                    reject(err);
                } else {
                    resolve(rows);
                }
            });
        });
    }

    /**
     * Retrieves apps from `info_table` that are free and meet the install threshold.
     * @param {boolean} free - Whether to include only free apps.
     * @param {number} number - Minimum number of installs.
     * @returns {Promise<Array<Object>>}
     */
    getAppsAtLeast(free, number) {
        const isFree = free == true ? 1 : 0;
        return new Promise((resolve, reject) => {
            this.db.all(`SELECT * FROM info_table WHERE minInstalls >= ? and free == ?`, [number, isFree], (err, rows) => {
                if (err) {
                    this.logger.error('Error fetching apps:', err.message);
                    reject(err);
                } else {
                    this.logger.info(`Found ${rows.length} apps with at least ${number} installs`);
                    resolve(rows);
                }
            });
        });
    }

    /**
     * Retrieves apps at a given depth and crawl status from `pids_table`.
     * @param {number} depth - BFS depth level.
     * @param {number} similarQueried - 0 = not yet queried, 1 = queried, -2 = error.
     * @returns {Promise<Array<Object>>}
     */
    getAppsByDepthAndFlag(depth, similarQueried) {
        return new Promise((resolve, reject) => {
            this.db.all(`SELECT * FROM pids_table WHERE depth = ? AND similar_queried = ?`, [depth, similarQueried], (err, rows) => {
                if (err) {
                    this.logger.error(`Failed to fetch apps with depth ${depth} and similar_queried ${similarQueried} from the database:`, err.message);
                    reject(err);
                } else {
                    resolve(rows);
                }
            });
        });
    }

    /**
     * Updates the `similar_queried` flag for an app.
     * @param {string} packageId - Package name.
     * @param {number} value - New flag value (e.g., 1 or -2).
     * @returns {Promise<void>}
     */
    updateSimilarQueried(packageId, value) {
        return new Promise((resolve, reject) => {
            this.db.run(`UPDATE pids_table SET similar_queried = ? WHERE package = ?`, [value, packageId], (err) => {
                if (err) {
                    this.logger.error(`Failed to update app ${packageId} with similar_queried ${value}:`, err.message);
                    reject(err);
                } else {
                    resolve();
                }
            });
        });
    }

    /**
     * Checks if a given app exists in the `pids_table`.
     * @param {string} packageId - Package name.
     * @returns {Promise<boolean>} - True if the app exists, false otherwise.
     */
    checkIfAppExists(packageId) {
        return new Promise((resolve, reject) => {
            this.db.get(`SELECT * FROM pids_table WHERE package = ?`, [packageId], (err, row) => {
                if (err) {
                    this.logger.error(`Error checking if app ${packageId} exists in the database:`, err.message);
                    reject(err);
                } else {
                    resolve(!!row); // Return true if the app exists
                }
            });
        });
    }

    /**
     * Inserts a new app into the `pids_table`.
     * @param {Object} app - App object from google-play-scraper.
     * @param {number} depth - BFS depth at which the app was discovered.
     * @param {number} similarQueried - Flag indicating crawl status.
     * @returns {Promise<void>}
     */
    insertApp(app, depth, similarQueried) {
        return new Promise((resolve, reject) => {
            this.db.run(`INSERT INTO pids_table (package, downloads, json, similar_queried, depth) VALUES (?, ?, ?, ?, ?) ON CONFLICT(package) DO NOTHING`,
                [app.appId, null, JSON.stringify(app), similarQueried, depth], (err) => {
                    if (err) {
                        this.logger.error(`Failed to insert app ${app.appId} with depth ${depth} and similar_queried ${similarQueried}:`, err.message);
                        reject(err);
                    } else {
                        resolve();
                    }
                });
        });
    }

    /**
     * Updates an app's `similar_queried` field to mark it as failed or errored.
     * @param {Object} app - App object containing the package name.
     * @param {number} depth - BFS depth of the app.
     * @param {number} errCode - Error code (typically -2).
     * @returns {Promise<void>}
     */
    updateAppError(app, depth, errCode) {
        return new Promise((resolve, reject) => {
            this.db.run(`UPDATE pids_table SET similar_queried = ? WHERE package = ?`, [errCode, app.package], (err) => {
                if (err) {
                    this.logger.error(`Failed to update app ${app.package} with similar_queried ${errCode}:`, err.message);
                    reject(err);
                } else {
                    resolve();
                }
            });
        });
    }


    /**
     * Inserts a list of apps into `pids_table`. Duplicate entries are ignored.
     * @param {Array<Object>} packageInfos - List of app objects from google-play-scraper.
     * @returns {Promise<number>} - Number of newly inserted packages.
     */
    savePackages(packageInfos) {
        return new Promise((resolve, reject) => {
            const stmt = this.db.prepare(`
            INSERT INTO pids_table (package, downloads, json, depth)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(package) DO NOTHING
        `);

            let insertedCount = 0;

            const tasks = packageInfos.map(packageInfo => {
                return new Promise((res, rej) => {
                    const packageId = packageInfo.appId;
                    const downloads = null;
                    const jsonString = JSON.stringify(packageInfo);

                    stmt.run([packageId, downloads, jsonString, 0], function (err) {
                        if (err) {
                            return rej(err);
                        }
                        if (this.changes > 0) {
                            insertedCount++;
                        }
                        res();
                    });
                });
            });

            Promise.all(tasks)
                .then(() => {
                    stmt.finalize((err) => {
                        if (err) return reject(err);
                        resolve(insertedCount);  // Only truly new rows
                    });
                })
                .catch(err => {
                    stmt.finalize(() => reject(err));
                });
        });
    }


    /**
     * Closes the database connection.
     * @returns {void}
     */
    close() {
        this.db.close((err) => {
            if (err) {
                this.logger.error('Error closing the database:', err.message);
            }
        });
    }
}
