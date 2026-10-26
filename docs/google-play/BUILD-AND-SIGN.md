# Build e firma per Google Play

La beta GitHub corrente usa target API 35 e firma debug. Non caricare quell’APK come pacchetto Play.

1. Portare una candidata separata a compile/target API 36 con toolchain compatibile e completare i test documentati. Conservare il pacchetto it.solphivia.mobile e incrementare il versionCode per ogni caricamento.
2. In Android Studio: Build → Generate Signed App Bundle / APK → Android App Bundle.
3. Creare o scegliere la chiave di caricamento in una cartella privata esterna al repository. Password e backup restano al titolare; non inviarli in chat e non inserirli in file versionati.
4. Selezionare release e generare AAB. Verificare che il pacchetto non sia debuggable, abbia versione/SDK corretti e contenga solo dati distribuiti intenzionalmente.
5. Configurare Play App Signing. L’upload key e la chiave che firma le installazioni distribuite da Play possono essere diverse.
6. Eseguire verifiche ELF/ZIP e prova runtime 16 KB; poi test interno e pre-launch report. Acquisire screenshot reali della stessa candidata.

La firma debug già installata sul telefono non è la firma di distribuzione Play. Non disinstallare l’app personale o promettere un aggiornamento diretto se le firme differiscono: predisporre backup cifrato e una migrazione provata separatamente.

Comando di packaging dopo aver completato SDK e configurazione privata di firma:

```text
./gradlew --no-daemon clean testReleaseUnitTest lintRelease bundleRelease
```

Senza configurazione di firma una build release può produrre un bundle non firmato: l’esistenza del file non significa che sia caricabile o approvato. Le chiavi non sono state generate da questo pacchetto di preparazione.
