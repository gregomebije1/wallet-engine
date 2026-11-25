package com.gregomebije.wallet;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WalletApplication {
	private static final Logger logger = LoggerFactory.getLogger(WalletApplication.class);

	
	public static void main(String[] args) {
		SpringApplication.run(WalletApplication.class, args);
	}

	/*

	@Bean
    public CommandLineRunner initDatabase(ServiceProviderRepository repo) {
        return args -> {
            // Your startup initialization logic goes safely here!
        };
    }

	@Override
	public void run(String...args) throws Exception {
		
		ServiceProvider flutterwave = new ServiceProvider("Flutterwave","Fund Transfer", 1234, 2000);
		ServiceProvider glade = new ServiceProvider("Glade","Fund Transfer", 1234, 2000);
		ServiceProvider aedcElectricity = new ServiceProvider("AEDC", "Electricity", 1234, 2000);
		ServiceProvider mtnData = new ServiceProvider("MTN", "Data", 2000, 2010);
		ServiceProvider mtnAirtime = new ServiceProvider("MTN", "Airtime", 2000, 2010);
		serviceProviderRepository.saveAll(Arrays.asList(flutterwave, glade, aedcElectricity, mtnData, mtnAirtime));
		logger.info("Added service providers");

		Service funds = new Service("Fund Transfer", flutterwave);
		Service electricity = new Service("Electricity", aedcElectricity);
		Service airtime = new Service("Airtime", mtnAirtime);
		Service data = new Service("Data", mtnData);
		serviceRepository.saveAll(Arrays.asList(funds, electricity, airtime, data));
		logger.info("Added services");
		

		// Fetch all services and log to console
			 Becauase I am in a command line runner, Open Session in View (OSIV) is disabled.
		   This means that when I call findAllwithProvider(), it opens a new EntityManager/Hibernate Session,
		   begins a transaction, runs your query, commits the transaction and closes the session — all before the method 
		   even returns control to your code. So by the time findAllWithProvider() 
		   returns the List<Service> to your run() method, the session is already gone. 
		   Any field access you do after that point (like service.getServiceProvider().getPlatformName()) has no session 
		   to lazily fetch data. JOIN FETCH (Not native sql) works around this: it doesn't keep the session open longer — 
		   it just makes sure everything you need is loaded before the session closes, in that single query, 
		   so nothing needs lazy fetching afterward. Also wrapping your own method in @Transactional also works: 
		   if you put @Transactional on your run() method (or a service method containing the whole flow).
		   A SQL JOIN controls what the database returns. JOIN FETCH / @EntityGraph control what Hibernate populates 
		   in your Java objects. Native SQL JOIN — joins the tables at the DB level, but doesn't reliably tell Hibernate's object layer to 
		   eagerly populate the Java association. You can get real joined rows back yet still end up with a lazy proxy on 
		   the entity
		

		for (Service service : serviceRepository.findAllWithProvider()) {
			logger.info("name: {}, provider: {}", service.getName(), service.getServiceProvider().getPlatformName());
		}	
	}
	*/
}
