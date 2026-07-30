# DIAGNOSTIC — ScreenBrightness

Constat factuel établi le **2026-07-30**, avant toute modification du code.
Rien n'a été corrigé : ce document ne fait que consigner l'état trouvé.

Ticket : **GEN-26** (epic GEN-25).

## Conditions du diagnostic

| | |
|---|---|
| Commit examiné | `7524dc6` — *Bump API to 36 and version to 1.1*, 2025-09-14 |
| JDK | OpenJDK 17.0.12 (JetBrains Runtime) |
| SDK Android | plateformes 31 à 37 installées, dont `android-36` |
| Commandes | `./gradlew assembleDebug lint testDebugUnitTest --no-daemon --continue` |

Le rapport distingue systématiquement ce qui a été **prouvé à l'exécution** de ce qui a été
**lu dans le code sans être exécuté**. L'application n'a pas été lancée sur un appareil.

---

## 1. Blocage de build — un seul, et il est gratuit

`./gradlew assembleDebug` échoue immédiatement, sur un clone neuf :

```
FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:processDebugGoogleServices'.
> File google-services.json is missing.
  The Google Services Plugin cannot function without it.
  Searched locations: app/src/debug/google-services.json, app/src/google-services.json,
  app/google-services.json
```

`google-services.json` est dans le `.gitignore` (ligne 30) et **n'a jamais été commité**
(vérifié sur tout l'historique, toutes branches). Le comportement est donc volontaire côté
secret — mais il rend le dépôt **non buildable par quiconque le clone**, y compris son auteur
sur une autre machine.

**Le point important : cette dépendance ne sert à rien.**

Une recherche sur `Firebase`, `firebase` et `Analytics` dans l'intégralité de `app/src` ne
renvoie **aucune occurrence**. Le plugin `com.google.gms.google-services`, `firebase-bom` et
`firebase-analytics` sont déclarés dans le build, et aucun code ne les appelle.

À noter tout de même avant de trancher : Firebase Analytics s'initialise seul, via un
`ContentProvider` déclaré par la bibliothèque, sans qu'une ligne de code soit nécessaire.
L'application **collecte donc bien des données analytiques** — ce que `privacy.md` mentionne.
Le choix est donc réel :

* soit Firebase est voulu → fournir un `google-services.json` et documenter comment l'obtenir ;
* soit il ne l'est pas → retirer le plugin et les deux dépendances, et le dépôt devient
  buildable immédiatement, sans aucun fichier de configuration.

## 2. État du build, une fois le blocage contourné

Contournement de diagnostic uniquement : un `google-services.json` factice a été déposé le temps
d'un build, puis supprimé. Il n'est ni commité ni conservé.

| Tâche | Résultat |
|---|---|
| `assembleDebug` | ✅ **passe** — APK debug produit |
| `lint` | ✅ passe — **0 erreur, 61 avertissements** |
| `testDebugUnitTest` | ❌ **échec — 2 tests, 1 en échec** |

Autrement dit : hors le fichier manquant, **le code compile sans rien changer**. Le brief de
l'epic anticipait une chaîne de build à reconstruire (jcenter mort, dépendances abandonnées) ;
ce n'est pas le cas. Le projet utilise déjà les version catalogs, Gradle 8.9, et ne référence
que `google()` et `mavenCentral()`.

### Avertissement de compatibilité

```
WARNING: We recommend using a newer Android Gradle plugin to use compileSdk = 36
This Android Gradle plugin (8.7.0-rc01) was tested up to compileSdk = 35.
```

Le commit `7524dc6` a monté `compileSdk`/`targetSdk` à 36 sans monter l'AGP. De plus, la version
utilisée est une **release candidate** (`8.7.0-rc01`), pas une version stable. Lint signale que
`9.3.1` est disponible.

### Avertissements de compilation Kotlin

```
MainActivity.kt:170  'enablePendingPurchases()' is deprecated
OverlayService.kt:159 'Theme_Holo_NoActionBar_Fullscreen' is deprecated
OverlayService.kt:178 'systemWindowInsetTop' is deprecated
OverlayService.kt:195 'FLAG_FULLSCREEN' is deprecated
```

### Lint

61 avertissements, dont **56 sont des dépendances périmées** (`GradleDependency`,
`AndroidGradlePluginVersion`). Les 5 autres : ressources inutilisées (7 couleurs du gabarit
Android Studio, 4 chaînes) et un dossier `mipmap-anydpi-v26` redondant puisque `minSdk = 26`.

Aucune erreur lint. Aucun avertissement de sécurité.

---

## 3. Le test qui échoue — un vrai bug, pas un test mal écrit

```
ColorTest > test color int conversion work correctly FAILED
    java.lang.AssertionError at ColorTest.kt:58
```

La ligne 58 est `assert(colorMid == reColorMid)` : un aller-retour `toLong()` → `fromLong()`
sur une couleur à saturation 0,5.

**Cause.** `Color.toLong()` encode chaque composante sur 8 bits (`(saturation.red * 255).toLong()`),
tandis que `Color.Saturation` arrondit ses accesseurs à **3 décimales** (`.round(3)`). Un octet ne
peut représenter que des multiples de 1/255 ≈ 0,00392 : 0,5 devient 127, puis 127/255 = 0,498.
L'égalité échoue légitimement.

**Conséquence pour l'utilisateur.** La couleur du filtre est persistée dans les SharedPreferences
via ce `toLong()`. À chaque rechargement, la teinte choisie est restituée **avec une dérive**.
Le test dit vrai ; c'est le code qui est en tort.

### Un second défaut, sur le même fichier, non couvert par les tests

```kotlin
val ColorSaver: Saver<Color, Long> = Saver(
    save    = { it.hashCode().toLong() },   // ← hashCode, pas toLong
    restore = { Color.fromLong(it) }        // ← attend un encodage ARGB
)
```

`save` sérialise un **hash**, `restore` désérialise un **encodage couleur**. Les deux ne sont pas
le même espace de valeurs. Le commentaire du code affirme « Color packs itself into a Long ARGB »,
ce qui est faux ici. Toute restauration d'état passant par ce `Saver` produit une couleur
arbitraire. Non détecté par les tests, qui ne couvrent pas ce chemin.

À signaler aussi : `Color.hashCode()` contient un `println("Color hashCode: …")`. `hashCode` est
appelé très fréquemment ; c'est de la pollution de log en production.

Le second test du fichier, `test rounding`, passe. Le premier test calcule `reColorMid2` sans
jamais l'utiliser dans une assertion — couverture incomplète, à reprendre en GEN-30.

---

## 4. Inventaire du code, écran par écran

Le module `app` fait **105 Ko de Kotlin** répartis en 20 fichiers. Aucun autre module.

| Écran / composant | Fichier | État observé |
|---|---|---|
| Activité principale | `MainActivity.kt` (13 Ko) | permissions, billing, ads, préférences, composition |
| Écran d'accueil | `views/MainScreen.kt` (12 Ko) | navigation entre les trois onglets |
| Luminosité | `views/BrightnessScreen.kt` (6,6 Ko) | curseur d'assombrissement |
| Couleur | `views/ColorScreen.kt` (21 Ko) | roue chromatique, teintes prédéfinies |
| Programmation | `views/SchedulerScreen.kt` (7,9 Ko) | heure + jours de la semaine |
| Popup d'information | `views/InformationsPopup.kt` (3,9 Ko) | texte d'explication publicité |
| Service de surdimpression | `OverlayService.kt` (16 Ko) | vue plein écran + notification permanente |
| Planification | `AlarmScheduler.kt`, `AlarmReceiver.kt`, `BootReceiver.kt` | alarmes système |
| Modèle couleur | `Color.kt` (5,5 Ko) | conversions, encodage `Long` |
| Publicité | `AdsScreen.kt` (2,8 Ko) | **jamais appelé** — voir §6 |
| Constantes | `keys.kt` | clés de préférences et d'intents, aucun secret |
| Code mort | `Config.kt` | composable `Configs()` au corps vide |

---

## 5. Défauts fonctionnels relevés à la lecture

Non vérifiés à l'exécution — l'application n'a pas été lancée. Classés par gravité.

### 5.1 Tous les crashs sont avalés silencieusement

`Application.kt` installe un gestionnaire d'exceptions **au corps vide** :

```kotlin
Thread.setDefaultUncaughtExceptionHandler(object : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) { }
})
```

Aucune trace, aucune remontée, et le processus n'est pas terminé — sur le thread principal,
l'application reste dans un état indéterminé plutôt que de fermer proprement. C'est aussi ce qui
rendrait tout rapport de plantage inexploitable si l'on en branchait un. À traiter avant toute
campagne de correction : en l'état, **aucun bug ne se signale**.

### 5.2 Le service de surdimpression ne survit pas à la fermeture de l'application

`MainActivity.onDestroy()` appelle `stopService(OverlayService)`, et `onResume()` fait un
`stopService` suivi d'un `startService`. Pour une application dont la fonction même est de
maintenir un filtre à l'écran en continu, c'est une contradiction : le filtre disparaît dès que
l'utilisateur ferme l'application, alors que le service est déclaré `START_STICKY`.

### 5.3 La programmation horaire ne fonctionne qu'une seule fois

`AlarmScheduler.scheduleOverlay()` utilise `alarmManager.setExact()`, qui est une alarme **unique**.
Aucun code ne la reprogramme après déclenchement : ni `AlarmReceiver`, ni le service, ni l'activité.
La planification annoncée dans le README comme récurrente (« Schedule automatic brightness changes »)
s'exécute donc **une fois par jour de semaine sélectionné, puis ne se reproduit jamais**.

Deux défauts connexes :

* `setExact` n'est pas `setExactAndAllowWhileIdle` : en mode Doze, l'alarme ne se déclenche pas.
* `AlarmReceiver` traite l'arrêt programmé par `stopService()`, ce qui **détruit le service** au
  lieu de simplement retirer la surdimpression — la notification de contrôle disparaît avec lui.

### 5.4 Après un redémarrage, l'horaire d'arrêt est inventé

`BootReceiver` restaure l'heure de début depuis les préférences, mais l'heure de fin n'y est
jamais enregistrée. Il la reconstruit en ajoutant **8 heures en dur** :

```kotlin
val stopTime = Calendar.getInstance().apply { …; add(Calendar.HOUR_OF_DAY, 8) }
```

La planification restaurée ne correspond donc pas à celle réglée par l'utilisateur.

### 5.5 Le service de premier plan risque de planter sur Android 14+

`OverlayService.onStartCommand()` appelle `startForeground(1, createNotification())` **sans type
de service**. Depuis Android 14, un service `specialUse` doit passer son type explicitement, faute
de quoi le système lève `MissingForegroundServiceTypeException`. Avec `targetSdk = 36`, cette
règle s'applique. À confirmer sur appareil — c'est le premier point à vérifier en GEN-29.

### 5.6 Demandes de permissions mal formées

`MainActivity.askPermissions()` boucle sur une liste et appelle `requestPermission.launch()` pour
chaque entrée. Deux problèmes :

* `SYSTEM_ALERT_WINDOW`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` et
  `FOREGROUND_SERVICE_SPECIAL_USE` **ne sont pas des permissions d'exécution**. Les demander par
  ce canal n'a aucun effet.
* Android ne traite qu'une demande à la fois : les lancements successifs s'écrasent.

Par ailleurs, `checkNotificationPermission()` ouvre les réglages système du téléphone, et
`askPermissions()` peut lancer l'écran d'alarmes exactes — les deux à chaque `onCreate`.
L'utilisateur est expédié dans les réglages dès l'ouverture de l'application.

### 5.7 Fuites de contexte

`MainActivity` et `Application` exposent chacun leur instance dans un `companion object`
(`lateinit var instance`). Conserver une `Activity` dans une référence statique la maintient en
mémoire au-delà de son cycle de vie.

### 5.8 Code inachevé laissé en place

* `billingClient` est construit dans `onCreate` mais `startConnection()` n'est jamais appelé ;
  `purchasesUpdatedListener` a un corps vide avec le commentaire « To be implemented in a later
  section ». Le message du commit `7fa06c3` le confirme : « App fully working but no ad and
  subscription ».
* `brightnessReceiver` est déclaré, son enregistrement est **commenté** dans `onStart`/`onStop`.
* `sendBrightnessToActivity()` n'est jamais appelée (ses deux appels sont commentés) : la
  surdimpression ne peut pas renvoyer l'état de luminosité à l'interface.
* `onNewIntent()` ne fait qu'appeler `super`.
* `Config.kt` contient un composable vide.

---

## 6. Publicité — l'écart entre ce qui est annoncé et ce qui existe

Le README annonce « Ads: Includes ad support ». Dans les faits :

* `AdsScreen.kt` définit un composable `MainActivity.AdsScreen()` **qui n'est appelé nulle part**.
  Aucune publicité n'est donc jamais affichée.
* `MobileAds.initialize(this) {}` est bien appelé dans `MainActivity.onCreate` (ligne 177), et
  l'`APPLICATION_ID` AdMob figure dans le manifeste. Le SDK s'initialise donc pour rien.
* **Aucune gestion du consentement (UMP / `ConsentInformation`)** n'est présente. Elle est exigée
  par Google pour diffuser dans l'EEE.
* Les identifiants AdMob **réels** sont en clair dans le dépôt :
  * `AndroidManifest.xml` ligne 37 — `APPLICATION_ID`
  * `AdsScreen.kt` ligne 25 — identifiant de bloc *rewarded*

  Ces identifiants ne sont pas des secrets au sens strict : ils sont extractibles de tout APK
  publié. Le sujet relève de la conformité (identifiants de test en debug), pas de la fuite —
  c'est l'objet de GEN-32.

**Point à surveiller si le composable est un jour branché tel quel.** La logique actuelle affiche
la publicité *rewarded* dès qu'elle est chargée, sans action de l'utilisateur, et remet
`rewardedAd` à `null` à la fermeture — ce qui relance le `LaunchedEffect`, donc un nouveau
chargement, donc un nouvel affichage. Les règles AdMob imposent qu'une *rewarded* soit déclenchée
par l'utilisateur ; en boucle et sans consentement, c'est un motif de suspension de compte. À
reprendre entièrement plutôt qu'à activer.

---

## 7. Hygiène de dépôt

### Le dépôt pèse 94 Mo pour 105 Ko de code

`media/exemple.gif` fait à lui seul **46,9 Mo** (le reste étant l'historique git de ce même
fichier). Tout clone paie ce prix. Un GIF de cette taille s'affiche mal sur GitHub. À convertir
en MP4/WebM, ou à réduire, sachant que la purge de l'historique serait nécessaire pour récupérer
l'espace — arbitrage à faire en GEN-28.

### Le reste est sain

* `.gitignore` correct et complet (Gradle, IDE, keystores, `google-services.json`, profilage).
* Aucun `.idea/`, `build/` ou `.gradle/` suivi par git.
* `LICENSE-2.0.txt` présent (Apache 2.0), référencé par le README.
* **Aucun secret dans le dépôt ni dans son historique** : `keys.kt`, malgré son nom, ne contient
  que des constantes de clés de `SharedPreferences` et d'`Intent`. Ce dépôt n'apparaît pas dans
  la checklist de révocation GEN-4.
* Traductions **complètes** : 46 chaînes en anglais, 46 en français, aucune clé manquante d'un
  côté ou de l'autre.

### Aucune intégration continue

Pas de `.github/workflows`. Rien ne signale une régression — c'est l'objet de GEN-31.

---

## 8. Points bloquants pour une publication Play Store

À traiter en GEN-33, relevés ici pour mémoire.

1. **`privacy.md` se termine par une phrase de robot conversationnel** laissée telle quelle :
   *« Would you like this localized for GDPR, CCPA, or another regulation? »*. Le document est
   destiné à être publié comme politique de confidentialité officielle.
2. **La politique déclare des collectes qui n'existent pas** : nom, adresse e-mail, données de
   localisation, cookies. L'application ne collecte rien de tout cela. Une politique qui
   sur-déclare entre en contradiction avec le questionnaire « sécurité des données » de la Play
   Console — motif de rejet.
3. **« not intended for children under 3 »** — vraisemblablement 13. À corriger avant de remplir
   la déclaration de tranche d'âge.
4. `FOREGROUND_SERVICE_SPECIAL_USE` exige une propriété
   `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` dans le manifeste, justifiant l'usage. **Elle est
   absente.** Google rejette les dépôts qui ne la fournissent pas.
5. `SYSTEM_ALERT_WINDOW` demande une justification explicite dans la fiche — motif de rejet fréquent,
   déjà relevé dans le ticket GEN-33.
6. Aucune configuration de signature release : `app/build.gradle.kts` ne définit pas de
   `signingConfig`, et `isMinifyEnabled = false` sur `release`.
7. Faute d'accord dans `privacy.md` : « **ScreenBrightness** We respects your privacy ».

---

## 9. Ce qui n'a pas été vérifié

Pour ne pas laisser croire à une couverture plus large qu'elle ne l'est :

* L'application **n'a pas été exécutée**, ni sur émulateur ni sur appareil. Tous les points du §5
  proviennent de la lecture du code.
* Le comportement réel de la surdimpression, du rendu couleur (`BlendMode.HARD_LIGHT`) et de la
  notification n'a donc pas été observé.
* `./gradlew assembleRelease` n'a pas été tenté (pas de configuration de signature).
* Les tests instrumentés (`ExampleInstrumentedTest`, le gabarit par défaut) n'ont pas été lancés.

---

## Ordre de traitement suggéré

1. **Trancher sur Firebase** (§1) — c'est ce qui débloque le build pour tout le monde, et cela
   conditionne aussi ce que doit dire `privacy.md`.
2. **Retirer le gestionnaire d'exceptions vide** (§5.1) — sans cela, aucun des bugs suivants ne se
   manifeste de façon observable.
3. Monter l'AGP vers une version stable compatible `compileSdk 36` (§2).
4. Vérifier sur appareil le démarrage du service de premier plan (§5.5), puis reprendre le cycle de
   vie du service (§5.2).
5. Reprendre la planification (§5.3, §5.4) — c'est la fonctionnalité la plus visiblement cassée.
6. Corriger l'encodage couleur (§3), en gardant le test existant comme critère.

---

*Rapport produit dans le cadre du chantier de remise au propre des dépôts GitHub (Jira GEN).*
