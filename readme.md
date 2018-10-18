# Sak

Sak er etablert som en erstatning for 'Sak-modulen' i GSAK, og tilbyr støttefunksjonalitet for å opprette- og søke opp saksreferanse som benyttes mot Joark for å :

* Journalføre på en Sak
* Hente journalførte dokumenter koblet mot en Sak

## Lokal utvikling

**Passord** og annen informasjon som kan være av sensitiv art, skal ikke sjekkes inn!

### Oppstart

Applikasjonen kjører opp lokalt ved å starte **DevJetty** i test-scope. 

### Logging

Ved kjøring av **DevJetty** benyttes **logback-test.xml** 

### Properties
Ved lokal kjøring er properties definert i **sak.properties**. 

### Testing

Enhetstester og api-tester (mot in memory-db) kjøres som standard ved **mvn test**. Det er i tillegg satt opp kjøring
av mutasjonstester som kan kjøres med mvn test -Pmutation-tests, og 

### Bygging

Applikasjonen pakkes til en 'fat-jar' vha maven-shade-plugin. Applikasjonen kan deretter kjøres opp lokalt på følgende måte:

java -jar <app>.jar 

Merk at konfigurasjon må være satt enten i sak.properties, som system.properties (-D) eller som env-variabler for at applikasjonen skal
starte. 

Applikasjonen kan også kjøres opp i docker (PS - Docker må kjøres mot remote host dersom man benytter windows utvikler-image):

docker build . 
docker run <app> -p<fri port på host>:8080

Her vil også properties enten måtte tilgjengeliggjøres i jar, eller det må sendes med env-variabler i run-kommando. 


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
