package com.swisscom.allegro.escomparison;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Requests;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.threadpool.ThreadPool.Names.INDEX;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void upsertProduct() throws Exception {
//		Product product = Product.newBuilder()
//				.setProductId(faker.number().randomNumber())
//				.setProductName(faker.company().name())
//				.setProductPrice(faker.number().randomDouble(2, 10, 100))
//				.setProductStatus(ProductStatus.InStock)
//				.build();
//		productDao.upsertProduct(product);
//		esClient.admin().indices().flush(Requests.flushRequest(INDEX)).actionGet();
//
//		GetResponse getResponse = esClient.prepareGet(INDEX, TYPE, String.valueOf(product.getProductId())).get();
//		JsonFormat.Parser jsonParser = injector.getInstance(JsonFormat.Parser.class);
//		Product.Builder builder = Product.newBuilder();
//		jsonParser.merge(getResponse.getSourceAsString(), builder);
//		assertThat(builder.build()).isEqualTo(product);
	}

}
