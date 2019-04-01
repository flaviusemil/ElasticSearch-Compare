# ElasticSearch-Compare

A simple application used to compare elasticsearch data.

## Build it

You can build it with the following command:
<code>mvn clean package</code>

This will generate a jar in <code>*target*</code> folder.

## Run it
The compiled jar is already executable so you can run it with `./` syntax.

Ex: ```./target/escomparison-0.0.1-SNAPSHOT.jar```

_**PS:** To compare all the values you might need to override the `application.maxValuesToCompare` property and set it to `0`_.

## Override parameters
The application is using SpringBoot so overriding parameters is very easy.

You can override any parameter from ```application.yml``` with the `--` followed by the parameter.

Example starting the application with parameter override: ```./target/escomparison-0.0.1-SNAPSHOT.jar --remote.host=127.0.0.1```

You can also override multiple parameters.

Ex: ```./target/escomparison-0.0.1-SNAPSHOT.jar --remote.host=127.0.0.1 --remote.elasticsearch.cluster.name=SOMENAME```

## Known problems
Currently the applications does not stop after all the items are compared, you have to stop it manually with `CTRL + C`.