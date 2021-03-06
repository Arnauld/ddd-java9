package fr.arolla.modec.acceptance;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import fr.arolla.modec.BusinessException;
import fr.arolla.modec.entity.*;
import fr.arolla.modec.repository.*;
import fr.arolla.modec.service.CartService;
import fr.arolla.modec.service.DeliveryService;
import fr.arolla.modec.service.OrderService;
import fr.arolla.modec.service.ProductService;
import fr.arolla.modec.service.system.Timestamp;
import io.cucumber.datatable.DataTable;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class StepDefs extends SpringBootBaseStepDefs {

    private ProductRepository productRepository;
    private ProductService productService;
    private CartService cartService;
    private Timestamp timestamp;
    private ShippingServiceRepository shippingServiceRepository;
    private OrderService orderService;
    private DeliveryService deliveryService;
    private CartId currentCartId;
    private OrderId currentOrderId;
    private DeliveryId currentDeliveryId;

    public StepDefs(ProductRepository productRepository, Timestamp timestamp, ShippingServiceRepository shippingServiceRepository, CartRepository cartRepository, CartLineRepository cartLineRepository, OrderRepository orderRepository, OrderLineRepository orderLineRepository) {
        this.productRepository = productRepository;
        this.productService = new ProductService(productRepository);
        this.cartService = new CartService(cartRepository, productRepository, cartLineRepository, shippingServiceRepository);
        this.timestamp = timestamp;
        this.shippingServiceRepository = shippingServiceRepository;
        this.orderService = new OrderService(cartRepository, timestamp, orderRepository, orderLineRepository);
        this.deliveryService = new DeliveryService(orderRepository);
    }

    @TestConfiguration
    static class TestContextConfiguration {

        @Bean
        public Timestamp timestamp() {
            return Mockito.mock(Timestamp.class);
        }
    }

    @Given("^now is \"([^\"]*)\"$")
    public void nowIs(String stringDate) throws Throwable {
        Calendar now = new GregorianCalendar();
        now.setTime(Date.from(Instant.from(ZonedDateTime.parse(stringDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME))));
        Mockito.when(timestamp.getCurrentDate()).thenReturn(now);
    }

    @Given("^\"([^\"]*)\" as default locale$")
    public void asDefaultLocale(String arg0) throws Throwable {
        //Locale features not implemented
    }

    @Given("^the following catalog:$")
    public void theFollowingCatalog(DataTable products) throws Throwable {
        productRepository.deleteAll();
        List<Map<String, String>> lines = products.asMaps();
        for (Map<String, String> line : lines) {
            productRepository.save(new Product(new Sku(line.get("SKU")), line.get("name"), line.get("description"), new Weight(Float.parseFloat(line.get("weight")))));
        }
    }

    @Given("^the following shipping services:$")
    public void theFollowingShippingServices(DataTable shippingServices) throws Throwable {
        shippingServiceRepository.deleteAll();
        List<Map<String, String>> lines = shippingServices.asMaps();
        for (Map<String, String> line : lines) {
            shippingServiceRepository.save(new ShippingService(line.get("code"), line.get("carrier"), line.get("level")));
        }
    }

    @Given("^a new cart is created$")
    public void aNewCartIsCreated() throws Throwable {
        this.currentCartId = cartService.createCart();
    }

    @Transactional
    @Given("^product \"([^\"]*)\" is added to this cart$")
    public void productIsAddedToThisCart(String sku) throws Throwable {
        cartService.addToCart(this.currentCartId, new Sku(sku), new Quantity(1));
    }

    @Transactional
    @Given("^\"([^\"]*)\", \"([^\"]*)\", \"([^\"]*)\", \"([^\"]*)\" in \"([^\"]*)\" is set as the shipping address of this cart$")
    public void inIsSetAsTheShippingAddressOfThisCart(String fullName, String line1, String city, String zipCode, String isoCountryCode) throws Throwable {
        cartService.setShippingAddress(this.currentCartId, fullName, line1, city, zipCode, isoCountryCode);
    }

    @Transactional
    @Then("^product catalog should contain:$")
    public void productCatalogShouldContain(DataTable products) throws Throwable {
        List<Map<String, String>> actualList = new ArrayList<>();

        for (Product product : productService.getList()) {
            Map<String, String> productMap = new HashMap<>();
            productMap.put("SKU", product.getSku().getSku());
            productMap.put("name", product.getName());
            actualList.add(productMap);
        }
        assertThat(actualList).isEqualTo(products.asMaps());
    }

    @Transactional
    @Then("^cart content should contain:$")
    public void cartContentShouldContain(DataTable cartLines) throws Throwable {
        List<Map<String, String>> actualList = new ArrayList<>();

        for (CartLine cartLine : cartService.getLines(this.currentCartId)) {
            Map<String, String> cartLineMap = new HashMap<>();
            cartLineMap.put("SKU", cartLine.getProductSku().getSku());
            cartLineMap.put("name", cartLine.getProductName());
            cartLineMap.put("quantity", cartLine.getQuantity().toString());
            actualList.add(cartLineMap);
        }
        assertThat(actualList).isEqualTo(cartLines.asMaps());
    }

    @Transactional
    @Then("^the shipping services available for this cart should be:$")
    public void theShippingServicesAvailableForThisCartShouldBe(DataTable shippingServices) throws Throwable {
        List<Map<String, String>> actualList = new ArrayList<>();

        for (ShippingService shippingService : cartService.getShippingServices(this.currentCartId)) {
            Map<String, String> shippingServiceMap = new HashMap<>();
            shippingServiceMap.put("code", shippingService.getCode());
            shippingServiceMap.put("label", shippingService.getCarrier() + " " + shippingService.getLevel());
            actualList.add(shippingServiceMap);
        }
        assertThat(actualList).isEqualTo(shippingServices.asMaps());
    }

    @Transactional
    @And("^\"([^\"]*)\" with email address \"([^\"]*)\" is set as the recipient$")
    public void withEmailAdressIsSetAsTheRecipient(String fullName, String eMail) throws Throwable {
        cartService.setRecipient(currentCartId, fullName, eMail);
    }

    @Transactional
    @And("^order is validated$")
    public void orderIsValidated() throws Throwable {
        currentOrderId = orderService.createOrderFromCart(this.currentCartId);
    }

    @Transactional
    @And("^order should not be validated$")
    public void orderCannotBeValidated() throws Throwable {
        assertThatThrownBy(() -> orderService.createOrderFromCart(currentCartId)).isInstanceOf(BusinessException.class);
    }

    @Transactional
    @Then("^the historical orders for recipient \"([^\"]*)\" should be:$")
    public void theHistoricalOrdersForRecipientShouldBe(String eMail, DataTable orders) throws Throwable {
        List<Map<String, String>> actualList = new ArrayList<>();

        for (Order order : orderService.getOrdersForEMail(eMail)) {
            Map<String, String> orderMap = new HashMap<>();
            orderMap.put("creation date", new SimpleDateFormat("YYYY-MM-dd").format(order.getCreationDate().getTime()));
            orderMap.put("status", order.getStatus().toString());
            actualList.add(orderMap);
        }
        assertThat(actualList).isEqualTo(orders.asMaps());
    }

    @Transactional
    @And("^delivery is validated$")
    public void deliveryIsValidated() throws Throwable {
        currentDeliveryId = deliveryService.createDeliveryFromOrder(currentOrderId);
    }
}
