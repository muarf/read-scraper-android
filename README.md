# Read Scraper Android

Application Android pour scraper des articles de presse en utilisant l'API Read Scraper.

## Fonctionnalités

- ✅ Recherche d'articles par URL ou termes de recherche
- ✅ Suivi en temps réel du statut du scraping
- ✅ Téléchargement des PDFs générés
- ✅ Historique des articles
- ✅ Interface utilisateur moderne avec Jetpack Compose
- ✅ Navigation intuitive avec barre de navigation inférieure
- ✅ Gestion des clés API (temporaires ou permanentes)
- ✅ **Menu contextuel "Rechercher sur la presse"** : Sélectionnez du texte dans n'importe quelle app et choisissez "Rechercher sur la presse" pour lancer automatiquement un scraping
- ✅ Partage de texte : Partagez du texte depuis n'importe quelle app vers Presse Scraper

## Architecture

L'application utilise :
- **Kotlin** avec **Jetpack Compose** pour l'interface utilisateur
- **MVVM** (Model-View-ViewModel) pour l'architecture
- **Retrofit** pour les appels API REST
- **Coroutines** pour la gestion asynchrone
- **DataStore** pour le stockage des préférences

## Build avec GitHub Actions

Le projet est configuré pour être construit automatiquement avec GitHub Actions. Le workflow `.github/workflows/build.yml` s'exécute sur chaque push ou pull request.

### Résultat du build

À chaque build réussi, l'APK sera disponible dans les artefacts GitHub Actions.

## Configuration

L'application se connecte par défaut à l'API à l'adresse `http://104.244.74.191:5000`. Vous pouvez modifier cette URL dans les paramètres de l'application.

### Clé API

L'application peut :
- Obtenir automatiquement une clé API temporaire (valide 24h)
- Utiliser une clé API permanente configurée dans les paramètres

## Structure du projet

```
app/
├── src/main/
│   ├── java/com/readscraper/android/
│   │   ├── data/
│   │   │   ├── api/          # Service Retrofit
│   │   │   ├── model/         # Modèles de données
│   │   │   ├── preferences/  # Gestion des préférences
│   │   │   └── repository/   # Repository pattern
│   │   ├── ui/
│   │   │   ├── navigation/   # Navigation
│   │   │   ├── screen/       # Écrans Compose
│   │   │   ├── theme/        # Thème Material Design
│   │   │   └── viewmodel/    # ViewModels
│   │   └── MainActivity.kt
│   └── res/                  # Ressources Android
```

## Développement

### Prérequis

- Android Studio Hedgehog | 2023.1.1 ou supérieur
- JDK 17
- Android SDK (minSdk 24, targetSdk 34)

### Build local

```bash
./gradlew assembleRelease
```

L'APK sera généré dans `app/build/outputs/apk/release/app-release.apk`

## License

Ce projet est un client Android pour l'API Read Scraper.

