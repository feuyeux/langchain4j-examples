
import com.redis.testcontainers.RedisContainer;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.utility.DockerImageName;

import java.util.List;


public class RedisEmbeddingStoreExample {
    private static final Logger LOGGER = LogManager.getLogger(RedisEmbeddingStoreExample.class);

    public static void main(String[] args) {
        try (RedisContainer redis = new RedisContainer(DockerImageName.parse("redis/redis-stack-server:latest"))) {
            redis.start();

            EmbeddingStore<TextSegment> embeddingStore = RedisEmbeddingStore.builder()
                    .host(redis.getHost())
                    .port(redis.getFirstMappedPort())
                    .dimension(384)
                    .build();

            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            TextSegment segment1 = TextSegment.from("I like football.");
            Embedding embedding1 = embeddingModel.embed(segment1).content();
            embeddingStore.add(embedding1, segment1);

            TextSegment segment2 = TextSegment.from("The weather is good today.");
            Embedding embedding2 = embeddingModel.embed(segment2).content();
            embeddingStore.add(embedding2, segment2);

            Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();
            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 1);
            EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);
            // expected 0.8144288659095
            LOGGER.info("Score: {}", embeddingMatch.score());
            // expected "I like football."
            LOGGER.info("Embedded: {}", embeddingMatch.embedded().text());
        } catch (Exception e) {
            LOGGER.error("Error: {}", e.getMessage());
        }
    }
}
