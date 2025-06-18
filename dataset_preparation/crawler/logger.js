/**
 * Logger setup for the Google Play Store crawler.
 * 
 * This module exports a factory function `createLogger` that creates a Pino logger
 * configured to write to both the console and a log file. If no log file path is provided,
 * a timestamped file is created in the current module's directory.
 * 
 * Usage: Called by other modules to initialize logging.
 * 
 * @module logger.js
 * @author Philipp Beer
 */

import pino from 'pino';
import pkg from 'pino-multi-stream';
const { multistream } = pkg;
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const thisFileName = fileURLToPath(import.meta.url);
const thisDirName = path.dirname(thisFileName);

/**
 * Creates a Pino logger that logs to both stdout and a file.
 * 
 * @param {string} [logFilename] - Optional absolute or relative path to the log file.
 *                                 If not provided, a timestamped file is created in the current directory.
 * @returns {pino.Logger} A configured Pino logger instance.
 * 
 * @example
 * import { createLogger } from './logger.js';
 * const logger = createLogger('logs/output.log');
 * logger.info('Logger initialized');
 */
export function createLogger(logFilename) {
  if (!logFilename) {
    const date = new Date();
    logFilename = path.join(
      thisDirName,
      `log-${date.getDate()}-${date.getMonth() + 1}-${date.getFullYear()}.log`
    );
  }

  const fileStream = fs.createWriteStream(logFilename, { flags: 'a' });

  const streams = [
    { stream: process.stdout }, // log to the console
    { stream: fileStream } // log to the file
  ];

  return pino({ level: 'info' }, multistream(streams));
}