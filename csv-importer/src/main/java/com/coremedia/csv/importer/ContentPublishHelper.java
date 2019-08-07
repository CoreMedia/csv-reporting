package com.coremedia.csv.importer;

import com.coremedia.cap.common.FlushFailedException;
import com.coremedia.cap.common.SessionNotOpenException;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentException;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.Version;
import com.coremedia.cap.content.publication.PublicationException;
import com.coremedia.cap.content.publication.PublicationFailedException;
import com.coremedia.cap.content.publication.PublicationService;
import com.coremedia.cap.content.publication.PublicationSet;
import com.coremedia.cap.content.publication.results.PublicationResult;
import com.coremedia.cap.content.publication.results.PublicationResultItem;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Helper class to handle the content checkout/checkin and publishing.
 */
public class ContentPublishHelper {

    private ContentRepository contentRepository;
    private Logger logger;
    private List<String> invalidFileNameChars = Collections.singletonList("/");
    private List<String> invalidFileNames = Arrays.asList(".", "..");
    @NonNull
    private final Collection<String> warnings = Collections.synchronizedList(new ArrayList<String>());

    public ContentPublishHelper(ContentRepository contentRepository, Logger logger) {
        this.contentRepository = contentRepository;
        this.logger = logger;
    }

    /**
     * Performs a checkIn on all the given content objects using a batch process
     *
     * @param contents The collection of content objects to check-in
     */
    public void checkInAll(Collection<Content> contents) {
        new BatchTemplate<Content>(contentRepository, BatchTemplate.DEFAULT_BATCH_SIZE, logger) {
            @Override
            protected void process(com.coremedia.cap.undoc.content.ContentRepository.Batch batch, Content content) {
                if (null != content && content.isCheckedOut()) {
                    batch.checkIn(content);
                }
            }
        }.execute(contents);
    }

    /**
     * Approves and/or publishes updated content depending on state of prior version.
     *
     * @param contents The collection of content objects that were updated
     * @param autoPublish Whether content should automatically be published if prior version was published
     */
    public void applyPreviousState(Collection<Content> contents, boolean autoPublish) {
        try {
            contentRepository.getConnection().flush();
            if (contents.size() > 0) {
                List<Content> toBeApproved = new ArrayList<>();
                List<Content> toBePublished = new ArrayList<>();

                PublicationService publicationService = contentRepository.getPublicationService();
                // assemble list of content to be approved/published based on prior version
                for (Content content : contents) {
                    Version priorVersion = content.getCheckedOutVersion();
                    if (priorVersion == null)
                        continue;
                    if (publicationService.isPublished(priorVersion)) {
                        toBeApproved.add(content);
                        if(autoPublish) {
                            toBePublished.add(content);
                        } else {
                            logger.info("No autopublish specified, skipping automatic publication of updated content " + content.getId());
                        }
                    } else if (publicationService.isApproved(priorVersion)) {
                        toBeApproved.add(content);
                    }
                }
                // check in updated content
                checkInAll(contents);

                // approve content
                logger.info("Approving " + toBeApproved.size() + " documents");
                for(Content content : toBeApproved) {
                    Version version = content.getCheckedInVersion();
                    if(!publicationService.isPlaceApproved(content))
                        publicationService.approvePlace(content);
                    if(!publicationService.isApproved(version))
                        publicationService.approve(version);
                }

                // publish content
                Collection<Version> toBePublishedVersions = new ArrayList<>();
                for(Content content : toBePublished) {
                    Version version = content.getCheckedInVersion();
                    if(!publicationService.isPlaceApproved(content))
                        publicationService.approvePlace(content);
                    if(!publicationService.isPublished(version))
                        toBePublishedVersions.add(version);
                }
                if(autoPublish) {
                    logger.info("Publishing " + toBePublished.size() + " documents");
                    publish(toBePublishedVersions);
                }
            }
        } catch (SessionNotOpenException snoe) {
            logger.error("SessionNotOpenException: Can't establish session with the content repository.", snoe);
        } catch (FlushFailedException ffe) {
            logger.error("FlushFailedException: Can't flush connection with the content repository.", ffe);
        } catch (Exception e) {
            logger.error("Could not publish documents ", e);
        }
    }

    /**
     * Publish a collection of documents.
     *
     * @param toBePublishedContent The collection of CoreMedia documents to publish
     */
    private void publish(Collection<Version> toBePublishedContent) {
        PublicationService publicationService = contentRepository.getPublicationService();
        PublicationSet publicationSet = publicationService.createPublicationSet(toBePublishedContent);
        try {
            publicationService.publish(publicationSet);
        } catch (PublicationFailedException e) {
            logger.error(MessageFormat.format("Cannot bulk publish places and versions: {0}", e.getMessage()), e);
            PublicationResult publicationResult = e.getPublicationResult();
            for (PublicationResultItem item : publicationResult.getResults()) {
                if (item.isError()) {
                    logger.error("Publication Error " + item.toString());
                }
            }
        }
    }

    /**
     * Converts a string to a CoreMedia document acceptable name. The document name may not be empty, may not be '.'or '..',
     * may not contain '/' characters and must not be too long (234 digits).
     *
     * @param title                The potential title of the document
     * @param documentTitle_prefix The static prefix for a title which doesn't start with any type of a letter
     * @return A string ready to be used as a CoreMedia document name
     */
    public String getCleanCoreMediaDocumentTitle(String title, String documentTitle_prefix) {
        String fileName = replaceInvalidChars(title);
        if (StringUtils.isEmpty(fileName) || !startsWithLetter(fileName) || getInvalidFileNames().contains(fileName)) {
            fileName = documentTitle_prefix + fileName;
        }

        if (fileName.length() > 234) {
            fileName = fileName.substring(0, 233);
        }
        return fileName.trim();
    }

    /**
     * Removing invalid chars (defined in property invalidFileNameChars) in a string with.
     *
     * @param s The string to handle
     * @return The input string, stripped of all invalid chars
     */
    private String replaceInvalidChars(String s) {
        for (String ch : invalidFileNameChars) {
            s = s.replaceAll(ch, "");
        }
        return s;
    }

    /**
     * Returns true, if the given string starts with a letter (multi-language!).
     *
     * @param s The string to test
     * @return TRUE, if the given string starts with any type of a letter (multi-language!). FALSE otherwise.
     */
    public boolean startsWithLetter(String s) {
        return Pattern.compile("^\\p{L}").matcher(s).find();
    }

    /**
     * Check out the given content for editing.
     *
     * @param c The content object
     * @return The content if checkout was successful, else return null
     */
    Content checkOutContent(Content c) {
        if (c.isCheckedOutByCurrentSession()) {
            return c;
        }
        if (c.isCheckedOut()) {
            logger.warn("Content " + c.getId() + " is checked out, can't check it out");
            return null;
        }
        if (c.isDeleted()) {
            logger.warn("Content " + c.getId() + " is deleted , can't check it out");
            return null;
        }
        if (c.isDestroyed()) {
            logger.warn("Content is destroyed, can't check it out");
            return null;
        }

        try {
            c.checkOut();
            if (logger.isDebugEnabled()) {
                logger.debug("Checked out content " + c.getId());
            }
            return c;
        } catch (ContentException e) {
            logger.error("Error checking out content " + c.getId());
        }
        return null;
    }

    /**
     * Check in the given content, if it was checked out by the current session.
     *
     * @param c The content object
     */
    void checkInContent(Content c) {
        if (c != null && c.isCheckedOutByCurrentSession()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Checking in content " + c.getId());
            }
            c.checkIn();
        }
    }

    /**
     * Check in the given content, if it was checked out by the current session.
     *
     * @param c The content object
     * @return TRUE, if the publication was successfull, FALSE if not.
     */
    boolean publishContent(Content c) {
        if (c != null) {
            checkInContent(c);
            if (logger.isDebugEnabled()) {
                logger.debug("Published content " + c.getId());
            }
            try {
                if (!contentRepository.getPublicationService().isPublished(c)) {
                    if (!contentRepository.getPublicationService().isPlaceApproved(c)) {
                        contentRepository.getPublicationService().approvePlace(c);
                    }
                    if (!c.isFolder()) {
                        Version version = c.getCheckedInVersion();
                        if (version != null) {
                            contentRepository.getPublicationService().approve(version);
                        }
                    }
                    contentRepository.getPublicationService().publish(c);
                }
                return true;
            } catch (ContentException ce) {
                logger.error("ContentException: Could not publish content with id " + c.getId() + " (" + c.getPath() + ")", ce);
            } catch (PublicationFailedException pfe) {
                logger.error("PublicationFailedException: Could not publish content with id " + c.getId() + " (" + c.getPath() + ")", pfe);
                logger.error("isPublished " + contentRepository.getPublicationService().isPublished(c));
                logger.error("isPlaceApproved " + contentRepository.getPublicationService().isPlaceApproved(c));
                logger.error("isFolder " + c.isFolder());

            } catch (PublicationException pe) {
                logger.error("PublicationException: Could not publish content with id " + c.getId() + " (" + c.getPath() + ")", pe);
            } catch (Exception e) {
                logger.error("Exception: Could not publish content with id " + c.getId() + " (" + c.getPath() + ")", e);
            }
        }
        return false;
    }


    /**
     * Updates a CoreMedia content object with the given parameters. The child will not be
     * created if it does not exist.
     *
     * @param child      The CoreMedia content object for which to update properties
     * @param properties The properties for the new CoreMedia document
     * @return The newly created or updated content object.
     */
    @Nullable
    public Content updateContent(@Nullable Content child, @NonNull Map<String, Object> properties) {
        Content result = child;
        if (child == null || child.isDestroyed()) {
            logger.error("Content with has either does not exist or has been destroyed. Skipped writing properties" +
                    " to content.");
        } else {
            updateProperties(child, properties);
        }
        return result;
    }

    /**
     * Helper method to format a error message during a failure.
     *
     * @param msg The message which should contain insight about the nature of the issue
     */
    public void handleImportFailure(String msg) {
        handleImportFailure(msg, null);
    }

    /**
     * Helper method to format a error message during a failure.
     *
     * @param msg The message which should contain insight about the nature of the issue
     * @param e   The Exception object for a stack trace
     */
    public void handleImportFailure(String msg, @Nullable Exception e) {
        StringBuilder sb = new StringBuilder(msg);
        if (e != null) {
            logger.debug(sb.toString(), e);
            sb.append("\nCaused by: ").append(e).append("\nSee debug log for Exception stack trace");
        }

        warnings.add(sb.toString());

        logger.warn(sb.toString());
    }


    /**
     * Set the given properties in the given content.
     *
     * @param content    The content to update
     * @param properties The new properties
     */
    public void updateProperties(Content content, Map<String, Object> properties) {
        if (content == null || properties == null) {
            logger.error("Can't update contents properties if the content object (" + content + ") or the properties object (" + properties + ") are null!");
            return;
        }
        boolean needsCheckout = content.isCheckedIn();
        if (needsCheckout) {
            content.checkOut();
        }
        boolean success = false;
        try {
            content.setProperties(properties);
            success = true;
        } finally {
            if (!success && needsCheckout) {
                try {
                    content.revert();
                } catch (ContentException ce) {
                    logger.error("Can't revert the attempted but unsuccessful update of a content objects properties. (content id : " + content.getId() + " )");
                }
            }
        }
    }


    public List<String> getInvalidFileNames() {
        return invalidFileNames;
    }

    public Collection<String> getWarnings() {
        return warnings;
    }

    public ContentRepository getContentRepository() {
        return contentRepository;
    }

}
