package limdongjin.ignasr.handler;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import limdongjin.ignasr.MyTestUtil;
import limdongjin.ignasr.repository.IgniteRepository;
import limdongjin.ignasr.router.SpeechRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext
@ExtendWith(SpringExtension.class)
public class SpeechUploadHandlerIntegrationTest {

    // container port binding
    // https://github.com/testcontainers/testcontainers-java/issues/256#issuecomment-405879835
    static int hostPort = 20800;
    static int containerExposedPort = 10800;
    static Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerExposedPort)));
//    @LocalServerPort
//    private Integer port;

    @Container
    static GenericContainer<?> ignite = new GenericContainer<>(DockerImageName.parse("apacheignite/ignite:2.14.0"))
            .withExposedPorts(containerExposedPort)
            .withCreateContainerCmdModifier(cmd)
    ;

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));


    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("limdongjin.ignasr.kafka.bootstrapservers", kafkaContainer::getBootstrapServers);
        registry.add("limdongjin.ignasr.ignite.addresses", () -> "localhost:" + ignite.getMappedPort(10800).toString());
    }

    @Autowired
    private IgniteRepository igniteRepository;

    @Autowired
    private ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate;

    @Autowired
    private SpeechUploadHandler speechUploadHandler;

    @Test
    void is200OK(){
        var uuidString = UUID.randomUUID().toString();
        var label = "dong";
        BodyInserters.MultipartInserter multipartInserter = MyTestUtil.prepareMultipartInserter(new ClassPathResource("data/foo.wav"), uuidString, label);
        WebTestClient webTestClient = WebTestClient.bindToRouterFunction(new SpeechRouter().speechRoute(speechUploadHandler))
                .configureClient()
                .build();

        // Execute Request and Verify Response
        FluxExchangeResult<String> ret1 = webTestClient
                .post().uri("/api/speech/upload")
                .accept(MediaType.MULTIPART_FORM_DATA)
                .body(multipartInserter)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class);
        System.out.println(ret1);

        FluxExchangeResult<String> ret2 = webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class);
        System.out.println(ret2);
    }
}
