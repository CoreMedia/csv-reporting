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
     * Establishes whether a taxonomy in a specified tag hierarchy exists within the content
     *
     * @param taxonomyPath the path hierarchy of the desired taxonomy tag. Must be in the form of "/taxonomy/path/1/"
     * @param taxonomyRoot the root folder which holds the entire taxonomy hierarchy
     * @return the taxonomy specified in the path hierarchy. If no such taxonomy exists, returns null
     */
    public Content establishTax(String taxonomyPath, Content taxonomyRoot) {
        Content content = null;
        if (taxonomyPath != null && !taxonomyPath.isEmpty() && !taxonomyPath.equals("[]")) {
            List<String> taxonomyList = new ArrayList<>(Arrays.asList(taxonomyPath.split("/")));
            // clear out any empty entries that may exist from splitting the path
            taxonomyList.removeAll(Arrays.asList("", null, " "));

            // We query for our first taxonomy member in the hierarchy. In order for a tag to be a direct hierarchical
            // child of the root tag (i.e. Subjects), it must exist within the direct children (and not in a subfolder)
            // of that tag's respective folder
            Set<Content> children = taxonomyRoot.getChildren();
            for (Content taxonomyChild : children) {
                // We've hit our final taxonomy content - we know we've found it, or that it doesn't exist
                if (taxonomyList.isEmpty()) {
                    break;
                }
                else if (taxonomyChild.getName().equals(taxonomyList.get(0))) {
                    // Once we have our first tag we can pop that off the list and search in that tag's properties for
                    // the rest of the hierarchy
                    taxonomyList.remove(0);
                    content = getChildTaxonomy(taxonomyList, taxonomyChild);
                }
            }
        }
        return content;
    }

    /**
     * Takes the list of taxonomy names, order by heirarcharical relationship and the parent taxonomy for the path, and
     * finds the child tag at the end of the path.
     *
     * @param taxonomyList the list of the hierarchy, excluding the parent taxonomy
     * @param taxonomyRoot the parent taxonomy of the hierarchy
     * @return the taxonomy content object for the given taxonomy path of the tag parent. If not such tag exists,
     * returns null.
     */
    private Content getChildTaxonomy(List<String> taxonomyList, Content taxonomyRoot) {

        // Loop until we've hit the end of the hierarchy list
        int counter = 0;
        while (counter != taxonomyList.size() && taxonomyRoot != null) {

            // Gets the property "children" as opposed to getChildren(). 2 very different things...
            List<Content> children = (List<Content>) taxonomyRoot.get("children");
            for (Content childTaxonomy : children) {

                // reset the taxonomy root to null - in case the child of the next tag in the path does not exist
                taxonomyRoot = null;

                // If we have a match, continue to the next tag
                if (childTaxonomy.getName().equals(taxonomyList.get(counter))) {
                    taxonomyRoot = childTaxonomy;
                    counter++;
                    break;
                }
            }
        }
        return taxonomyRoot;
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
