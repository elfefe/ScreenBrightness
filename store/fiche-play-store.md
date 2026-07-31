# Dossier de publication Play Store — ScreenBrightness

Brouillon prêt à déposer. **Rien n'a été écrit sur la Play Console** : ce
document rassemble le contenu à valider avant tout dépôt.

Application : `com.elfefe.screenbrightness` — déjà publiée en production
(dernière version en ligne : 1.1 / versionCode 2). Le présent dossier concerne
la **mise à jour 1.2 / versionCode 3**.

---

## 1. Ce qui cloche dans la fiche actuelle

La fiche en ligne n'a **qu'une seule langue déclarée, `en-US`**, mais son
contenu — titre compris — est **entièrement en français**. Un utilisateur
anglophone voit donc une description française, alors que l'application, elle,
est traduite dans les deux langues.

Correction proposée : garder le texte français sous une fiche **`fr-FR`**, et
créer une vraie fiche **`en-US`** en anglais. Le titre passe de
« Luminosité d'écran » (actuellement sous en-US) à « Screen Brightness » pour
l'anglais.

---

## 2. Fiche française (`fr-FR`)

**Titre** (30 caractères max)
```
Luminosité d'écran
```

**Description courte** (80 caractères max)
```
Assombrissez l'écran sous le minimum du système et filtrez la lumière bleue.
```

**Description complète** (4000 caractères max)
```
Screen Brightness assombrit votre écran en dessous du minimum autorisé par le
système, à l'aide d'un filtre coloré posé par-dessus l'affichage.

Le curseur de luminosité d'Android s'arrête à un niveau encore trop lumineux
dans une pièce sombre. Screen Brightness va plus loin : il dessine une couche
translucide et teintée sur l'écran, ce qui permet aussi de réchauffer les
couleurs pour réduire la lumière bleue.

FONCTIONNALITÉS

• Assombrissement — réglez l'intensité du filtre depuis un curseur ou
  directement depuis la notification.
• Filtres colorés — choisissez une teinte sur une roue chromatique, ou l'un des
  préréglages : beige, chaud, froid, vert doux, gris foncé.
• Programmation horaire — activez et coupez le filtre à l'heure choisie, les
  jours de la semaine que vous voulez.
• Notification permanente — assombrissez, éclaircissez ou coupez le filtre sans
  ouvrir l'application.

RESPECT DE LA VIE PRIVÉE

Aucun compte, aucun serveur. Vos réglages restent sur l'appareil. L'application
ne lit jamais le contenu de votre écran : le filtre est simplement posé par
dessus.

L'application est gratuite. Vous pouvez la soutenir en regardant une publicité,
mais uniquement si vous le demandez : rien ne s'affiche tout seul.

PERMISSIONS

• « Superposer aux autres applications » : indispensable pour dessiner le
  filtre. Sans elle, l'application ne peut pas fonctionner.
• « Alarmes et rappels » : pour déclencher le filtre à l'heure programmée.
```

**Nouveautés de la version 1.2** (500 caractères max)
```
• La programmation horaire fonctionne désormais de façon fiable et se répète
  chaque semaine.
• Le filtre reste actif après la fermeture de l'application.
• Correction de la couleur du filtre, qui pouvait dériver au redémarrage.
• Publicité de soutien entièrement facultative, avec recueil du consentement.
• Compatibilité Android 14 et 15.
```

---

## 3. Fiche anglaise (`en-US`)

**Title** (30 chars max)
```
Screen Brightness
```

**Short description** (80 chars max)
```
Dim your screen below the system minimum and filter out blue light.
```

**Full description** (4000 chars max)
```
Screen Brightness dims your display below the minimum Android allows, using a
tinted overlay drawn on top of the screen.

Android's own brightness slider stops at a level that is still too bright in a
dark room. Screen Brightness goes further: it draws a translucent, tinted layer
over the screen, so it can also warm the colours to cut blue light.

FEATURES

• Dimming — adjust the overlay's strength from a slider or straight from the
  notification.
• Colour filters — pick a tint from a colour wheel, or one of the presets:
  beige, warm, cool, soft green, dark grey.
• Scheduling — turn the overlay on and off at a chosen time, on the days of the
  week you want.
• Ongoing notification — dim, brighten or switch the overlay off without opening
  the app.

PRIVACY

No account, no server. Your settings stay on the device. The app never reads
what is on your screen: the overlay is simply drawn on top of it.

The app is free. You can support it by watching an ad, but only if you ask for
one — nothing is ever shown on its own.

PERMISSIONS

• "Display over other apps": required to draw the overlay. Without it the app
  cannot work.
• "Alarms & reminders": used to turn the overlay on at your scheduled time.
```

**What's new in 1.2** (500 chars max)
```
• Scheduling now works reliably and repeats every week.
• The overlay stays on after you close the app.
• Fixed the filter colour drifting after a restart.
• Fully optional support ad, with consent handling.
• Android 14 and 15 compatibility.
```

---

## 4. Déclaration « sécurité des données »

À remplir en cohérence avec le nouveau `privacy.md`.

**Collecte de données** : OUI, mais limitée.

| Type | Collecté | Partagé | Finalité | Facultatif |
|---|---|---|---|---|
| Journaux de plantage | Oui | Oui (Google/Firebase) | Diagnostic, stabilité | Non |
| Diagnostics (perf.) | Oui | Oui (Google/Firebase) | Stabilité | Non |
| Identifiant publicitaire | Oui | Oui (Google AdMob) | Publicité | **Oui** — seulement après consentement |

**Ne PAS déclarer** (l'application ne les collecte pas, contrairement à ce
qu'affirmait l'ancien privacy.md) : nom, e-mail, localisation, contacts,
photos, fichiers, messages, historique de navigation.

**Sécurité** : les données transitent chiffrées (HTTPS). L'utilisateur ne peut
pas demander la suppression, les données de plantage étant anonymes.

Politique de confidentialité à publier : le contenu de `privacy.md`, hébergé à
une URL publique (au choix : page GitHub du dépôt, ou fedacier.com).

---

## 5. Questions de contenu (règles Google)

* **Publicité** : déclarer « Contient des publicités » — OUI (AdMob).
* **Catégorie de contenu** : tout public. Pas de contenu sensible.
* **Permission `SYSTEM_ALERT_WINDOW`** : Google demande une justification dans
  la fiche de déclaration. Justification proposée : *« L'application dessine un
  filtre d'assombrissement par-dessus les autres applications ; c'est sa
  fonction principale, annoncée dès la description, et l'utilisateur l'active
  explicitement au premier lancement. »*
* **Service `specialUse`** : la propriété `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` est
  désormais dans le manifeste (ajoutée en GEN-29). Sa justification y figure.

---

## 6. Éléments graphiques

À vérifier / refaire — non traités dans ce dossier :

* Icône : déjà présente dans l'application (`ic_lower_brightness`).
* Captures d'écran téléphone : les captures en ligne datent de la 1.0/1.1 ; à
  refaire si l'interface a changé. Le GIF de démonstration (`media/exemple.gif`)
  peut servir de base à en extraire des images fixes.
* Image de mise en avant (1024 × 500) : à produire si absente.

---

## 7. Ce qui est prêt techniquement

* **AAB signé** : `app/build/outputs/bundle/release/app-release.aab`, 13 Mo,
  versionCode 3 / 1.2, signé avec la clé d'upload d'origine (vérifiée contre le
  certificat d'upload de Google).
* **Piste visée** : test interne, conformément au critère de fin de GEN-33.
* Clé et mot de passe sauvegardés dans Vault.
