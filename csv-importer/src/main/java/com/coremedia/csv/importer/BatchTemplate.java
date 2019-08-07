package com.coremedia.csv.importer;

import com.coremedia.cap.content.ContentRepository;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * Template for looping with a {@link com.coremedia.cap.undoc.content.ContentRepository.Batch batch}.
 */
public abstract class BatchTemplate<T> {

    private final ContentRepository contentRepository;
    private final int batchSize;

    public static final int DEFAULT_BATCH_SIZE = 1000;

    private Logger logger;

    public BatchTemplate(ContentRepository contentRepository, int batchSize, Logger logger) {
        this.contentRepository = contentRepository;
        this.batchSize = batchSize;
        this.logger = logger;
    }

    protected abstract void process(com.coremedia.cap.undoc.content.ContentRepository.Batch batch, T object);

    /**
     * Loop through the batch and execute the Batch job.
     *
     * @param objects The object - collection to split into batches
     */
    public void execute(Collection<T> objects) {
        com.coremedia.cap.undoc.content.ContentRepository undocRepository =
                (com.coremedia.cap.undoc.content.ContentRepository) contentRepository;
        int i = 0;
        com.coremedia.cap.undoc.content.ContentRepository.Batch batch = undocRepository.createBatch();
        for (T object : objects) {
            process(batch, object);
            if (++i % batchSize == 0) {
                batch.executeBatch();
                logger.debug("Batch " + i + "/" + objects.size() + "...");
                batch = undocRepository.createBatch();
            }
        }
        if (i % batchSize > 0) {
            batch.executeBatch();
            logger.debug("Last partial batch: " + i);
        }
    }
}
