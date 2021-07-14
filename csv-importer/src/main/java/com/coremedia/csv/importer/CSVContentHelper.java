package com.coremedia.csv.importer;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import org.slf4j.Logger;

import java.util.*;

/**
 * Performs operations for querying/fetching content from CoreMedia when updating content based on values from a CSV
 * file.
 */
public class CSVContentHelper {

    /**
     * If updated content should be automatically be published if prior version was published.
     */
    private boolean autoPublish;

    /**
     * Handles publishing content after their properties have been updated.
     */
    private ContentPublishHelper contentPublishHelper;

    /**
     * Constructor.
     *
     * @param autoPublish       if updated content should be automatically be published if prior version was published
     * @param contentRepository the current content repository which contains the content to be updated.
     * @param logger            logger from the tool using this helper class
     */
    public CSVContentHelper(boolean autoPublish, ContentRepository contentRepository, Logger logger) {
        this.autoPublish = autoPublish;
        this.contentPublishHelper = new ContentPublishHelper(contentRepository, logger);
    }

    /**
     * Imports the content by updating the content object.
     *
     * @param content          the content which to update with new properties
     * @param properties       The properties for the new CoreMedia document
     * @param importedContents The collection of content objects to handle
     */
    public void importContent(Content content, Map<String, Object> properties, Collection<Content> importedContents) {
        handleImportContent(content, properties, importedContents);
    }

    /**
     * Imports the content by updating the content object.
     *
     * @param properties       The properties for the new CoreMedia document
     * @param importedContents The collection of content objects to handle
     */
    public void handleImportContent(Content content, Map<String, Object> properties,
                                    Collection<Content> importedContents) {

        // At this point, we've already checked to make sure the content exists - so it will not create a new one
        Content result = contentPublishHelper.updateContent(content, properties);
        if (result != null) {
            importedContents.add(result);
        }
    }

    /**
     * Helper method that applies the previous approved/published state to updated content. The collection will be cleared
     * afterwards.
     *
     * @param importedContents The list to handle
     */
    public void applyPreviousState(Collection<Content> importedContents) {
        // publish the remaining documents
        if (!importedContents.isEmpty()) {
            contentPublishHelper.applyPreviousState(importedContents, autoPublish);
            importedContents.clear();
        }
    }

    /**
     * Flattens a map. All values will be transferred in one big list. Duplicates can occur.
     *
     * @param tagsMap The Map to flatten
     * @return A list with all the Map's values.
     */
    public List<Content> flattenTagsMap(Map<String, Set<Content>> tagsMap) {
        List<Content> flatList = new ArrayList<>();
        for (Set<Content> tagMap : tagsMap.values()) {
            flatList.addAll(tagMap);
        }
        return flatList;
    }

    /**
     * Establishes a transition lock based on the given path string
     *
     * @param path                 The (hopefully) unique identifier for each object in the lock
     * @param transitionLockByPath The map of synchronized objects
     * @return The lock object for the given path
     */
    public Object getTransitionLock(String path, Map<String, Object> transitionLockByPath) {
        synchronized (transitionLockByPath) {
            Object lock = transitionLockByPath.get(path);
            if (lock == null) {
                lock = new Object();
                transitionLockByPath.put(path, lock);
            }
            return lock;
        }
    }
}
