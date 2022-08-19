# Sak

Sak er etablert som en erstatning for 'Sak-modulen' i GSAK, og tilbyr støttefunksjonalitet for å opprette- og søke opp saksreferanse som benyttes mot Joark for å :

* Journalføre på en Sak
* Hente journalførte dokumenter koblet mot en Sak

## Lokal utvikling

**Passord** og annen informasjon som kan være av sensitiv art, skal ikke sjekkes inn!

### Oppstart

Spring boot applikasjon som kjøres opp ved å kjøre Application 

### Logging

Ved kjøring av **Spring Boot** benyttes **logback-test.xml** 

### Properties
Ved lokal kjøring er properties definert i **sak.properties**. 

### Testing

Enhetstester og api-tester (mot in memory-db) kjøres som standard ved **mvn test**. Det er i tillegg satt opp kjøring
av mutasjonstester som kan kjøres med mvn test -Pmutation-tests, og 

### Bygging
mvn clean install

### Swagger

1. Åpne denne url `<ingress>` i en nettleser
2. Når du kommer inn til nettleseren vil du se at du får feilmeldingen `Failed to load API definition`. Dette er fordi swagger pekker på en swagger.json konfigurasjon som er feil og ikke tilgjengelig.
3. For å sette korrekt konfigurasjon så vil du gå til feltet `explore` på nettsiden.
4. På dette feltet vil vi at swagger skal hente konfigurasjonen fra `sak`, dette gjøres med å skrive dette inn i feltet `<ingress>/api/openapi.json`.
5. Når du har tryket på `explore` knappen vil swagger hente konfigurasjonen fra applikasjonen.

### Deploy

Applikasjonen kjører på NAIS-plattformen (se https://confluence.adeo.no/pages/viewpage.action?pageId=210440645)

Når en branch pushes vil Jenkins pipeline sørge for at denne automatisk deployes til preprod. Se 'Jenkinsfile' for 
gjeldende oppsett, og https://github.com/navikt/jenkins-oppgavehandtering-pipeline for felles-funksjonalitet som 
er lagt til i jenkins og som benyttes av Jenkinsfile i Sak. 


## Øvrige av interesse

## OIDC
OIDC ID_token valideres når det mottas en Authorization-header med 'Bearer' (Se AuthenticationFilter). Siden Sak er en 
ren tjeneste, er det ikke satt opp standard redirect->callback->hent token-mekanisme. Det er etablert integrasjonstest
som kjører i Jenkins (der credentials er definert for clientId og clientSecret). 

Tjenesten støtter per i dag OpenAM som issuer
