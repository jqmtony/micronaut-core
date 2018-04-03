package io.micronaut.discovery.aws.route53.client;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.AWSServiceDiscoveryClient;
import com.amazonaws.services.servicediscovery.model.*;
import io.micronaut.configurations.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.discovery.DiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.discovery.aws.route53.Route53ClientDiscoveryConfiguration;
import io.micronaut.discovery.aws.route53.Route53DiscoveryConfiguration;
import io.micronaut.discovery.aws.route53.registration.EC2ServiceInstance;
import io.micronaut.http.client.Client;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


@Singleton
@Client(id = Route53ClientDiscoveryConfiguration.SERVICE_ID, path = "/", configuration = Route53ClientDiscoveryConfiguration.class)
@Requires(env= Environment.AMAZON_EC2)
@Requires(beans = Route53DiscoveryConfiguration.class)
@Requires(beans = AWSClientConfiguration.class)
@Requires(property = "aws.route53.discovery.enabled", value = "true", defaultValue = "false")
public class Route53AutoNamingClient implements DiscoveryClient {

    @Inject
    AWSClientConfiguration awsClientConfiguration;

    @Inject
    Route53ClientDiscoveryConfiguration route53ClientDiscoveryConfiguration;

    @Inject
    Route53DiscoveryConfiguration route53DiscoveryConfiguration;


    AWSServiceDiscovery discoveryClient;


    @Override
    public String getDescription() {
        return null;
    }


    @Override
    public Publisher<List<ServiceInstance>> getInstances(String serviceId) {
        if (discoveryClient==null) {
            discoveryClient = AWSServiceDiscoveryClient.builder().withClientConfiguration(awsClientConfiguration.clientConfiguration).build();
        }
        if (serviceId==null) {
            serviceId = route53ClientDiscoveryConfiguration.getAwsServiceId();  // we can default to the config file
        }

        ListInstancesRequest instancesRequest = new ListInstancesRequest().withServiceId(serviceId);
        ListInstancesResult instanceResult = discoveryClient.listInstances(instancesRequest);
        List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>();
        for (InstanceSummary instanceSummary : instanceResult.getInstances()) {
            try {
                String uri = "http://"+instanceSummary.getAttributes().get("URI");
                ServiceInstance serviceInstance = new EC2ServiceInstance(instanceSummary.getId(),new URI(uri)).metadata(instanceSummary.getAttributes()).build();
                serviceInstances.add(serviceInstance);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return Publishers.just(
                serviceInstances
        );

    }

    @Override
    public Publisher<List<String>> getServiceIds() {

        if (discoveryClient==null) {
            discoveryClient = AWSServiceDiscoveryClient.builder().withClientConfiguration(awsClientConfiguration.clientConfiguration).build();
        }

        ServiceFilter serviceFilter = new ServiceFilter().withName("NAMESPACE_ID").withValues(route53ClientDiscoveryConfiguration.getNamespaceId());
        ListServicesRequest listServicesRequest = new ListServicesRequest().withFilters(serviceFilter);
        ListServicesResult response = discoveryClient.listServices(listServicesRequest);
        List<ServiceSummary> services = response.getServices();
        List<String> serviceIds = new ArrayList<String>();
        for (ServiceSummary service : services) {
            serviceIds.add(service.getId());
        }
        return Publishers.just(
                serviceIds
        );
    }

    @Override
    public void close() throws IOException {
        discoveryClient.shutdown();
    }
}