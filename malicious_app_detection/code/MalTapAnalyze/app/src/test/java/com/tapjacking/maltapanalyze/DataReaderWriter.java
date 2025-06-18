package com.tapjacking.maltapanalyze;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to read and write data from and to the SQLite database.
 */
public class DataReaderWriter implements AutoCloseable {

    private Connection connection;
    private final String databaseUrl;

    /**
     * Creates a new {@link DataReaderWriter} instance.
     * @param database the path to the SQLite database.
     * @throws SQLException if an error occurs while connecting to the database.
     */
    public DataReaderWriter(String database) throws SQLException {
        this.databaseUrl = "jdbc:sqlite:" + database;
        connect();
    }

    /**
     * Connects to the SQLite database.
     * @throws SQLException if an error occurs while connecting to the database.
     */
    private void connect() throws SQLException {
        this.connection = DriverManager.getConnection(this.databaseUrl);
    }

    /**
     * Retrieves the unique animation hashes from the database.
     * @return a list of unique animation hashes.
     * @throws SQLException if an error occurs while retrieving the data from the database.
     */
    public List<String> getUniqueAnimations() throws SQLException {
        String sql = "SELECT DISTINCT hash FROM anim";
        List<String> hashes = new ArrayList<>();
        ResultSet resultSet = this.connection.createStatement().executeQuery(sql);
        while (resultSet.next()) {
            hashes.add(resultSet.getString("hash"));
        }
        return hashes;
    }

    /**
     * Retrieves the XML string of an animation from the database given its hash.
     * @param hash the hash of the animation.
     * @return the XML string of the animation or null if no animation with the given hash exists.
     * @throws SQLException if an error occurs while retrieving the data from the database.
     */
    public String getAnimationContent(String hash) throws SQLException {
        String sql = "SELECT content FROM anim WHERE hash = '" + hash + "'";
        return this.connection.createStatement().executeQuery(sql).getString(1);
    }

    /**
     * Retrieves the XML string of an interpolator from the database given its hash.
     * @param hash the hash of the interpolator.
     * @return the XML string of the interpolator or null if no interpolator with the given hash exists.
     * @throws SQLException if an error occurs while retrieving the data from the database.
     */
    public String getInterpolatorContent(String hash) throws SQLException {
        String sql = "SELECT content FROM interpolator WHERE hash = '" + hash + "'";
        return this.connection.createStatement().executeQuery(sql).getString(1);
    }

    /**
     * Saves the scores of an animation in the database.
     * @param hash the hash of the animation.
     * @param alphaScore the alpha score of the animation.
     * @param scaleScore the scale score of the animation.
     * @param animationLonger whether the animation is longer than 3 seconds.
     * @param content the XML content of the animation.
     * @throws SQLException if an error occurs while saving the data to the database.
     */
    public void saveScore(String hash, int alphaScore, int scaleScore, boolean animationLonger, String content) throws SQLException {
        maybeCreateScoreTable();
        String sql = "INSERT INTO score (hash, alpha_score, scale_score, animation_longer, content) VALUES ('" + hash + "', " + alphaScore + ", " + scaleScore + ", " + animationLonger + ", '" + content + "')";
        this.connection.createStatement().execute(sql);
    }

    /**
     * Saves the exception that occurred while analyzing an animation in the database.
     * @param hash the hash of the animation.
     * @param code the error code.
     * @param content the XML content of the animation.
     * @param exception the exception that occurred.
     * @throws SQLException if an error occurs while saving the data to the database.
     */
    public void saveScoreException(String hash, int code, String content, Exception exception) throws SQLException {
        maybeCreateScoreTable();
        String sql = "INSERT INTO score (hash, alpha_score, scale_score, animation_longer, content, e) VALUES ('" + hash + "', " + code + ", " + code + ", " + code + ", '" + content + "', '" + exception.getMessage() + "')";
        this.connection.createStatement().execute(sql);
    }

    /**
     * Creates the score table if it does not exist.
     * @throws SQLException if an error occurs while creating the table.
     */
    private void maybeCreateScoreTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS score (hash TEXT PRIMARY KEY, alpha_score INTEGER, scale_score INTEGER, animation_longer BOOLEAN, content TEXT, e TEXT)";
        this.connection.createStatement().execute(sql);

    }

    /**
     * Closes the connection to the database.
     */
    @Override
    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                // Problems closing the connection is not critical
            }
        }
    }

}
