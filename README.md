# 🎵 Audio Renamer - VirtualDJ Database Updater

[![Java](https://img.shields.io/badge/Java-17-007396?logo=java)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21+-0078D7?logo=java)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-C71A36?logo=apache-maven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Herramienta de escritorio que **renombré masivamente archivos de audio** usando heurística inteligente, extracción de BPM y validación interactiva con preview de audio. Sincroniza automáticamente los cambios con la base de datos de **VirtualDJ** (`database.xml`) sin corromper beatgrids ni hot cues.

---

## ✨ Características Principales

###  **Parsing Inteligente**
- **Extracción automática de BPM** (60-180) desde cualquier posición del nombre
- **Detección de Artista/Canción/Editor** usando:
  - Lista de 40+ artistas conocidos (Karol G, Bad Bunny, Shakira, etc.)
  - Patrones de colaboración (Ft, Feat, X, Vs)
  - Análisis de tags de versión (Remix, VIP, Edit, Intro)
  - Heurísticas de longitud y estructura
- **Limpieza automática** de basura (@spam, $, VIRUS, PUTEAR, puntos residuales)

### 🎧 **Validación Interactiva**
- **Ventana modal** cuando el nombre es ambiguo (>1 guion o sin estructura clara)
- **Mini reproductor de audio** integrado con:
  - Timeline/Seek bar funcional
  - Play/Pause/Stop
  - Volumen ajustable (30% por defecto)
  - Formato MM:SS / MM:SS
- **Pre-llenado inteligente** de campos basado en análisis heurístico

### 📁 **Organización Segura**
- **Carpeta espejo incremental**: `_RENOMBRADO`, `_RENOMBRADO_1`, etc.
- **Preservación de originales**: Nunca borra archivos fuente
- **Manejo de duplicados**: Sufijos automáticos `(1)`, `(2)`, `(3)`
- **Filtrado por extensión**: MP3, WAV, M4A, FLAC, OGG, AAC, WMA, MP4, MPEG, AIF, AIFF

### 🛡️ **Integración VirtualDJ Anti-Corrupción**
- ✅ Verificación de que **VirtualDJ esté cerrado** antes de escribir
- 💾 **Backup automático** con timestamp (`database.xml.backup.1234567890`)
- 📝 **Escritura atómica**: Archivo temporal → validación → reemplazo
- 🔍 **Validación XML** post-escritura para garantizar integridad
- 🎯 **Preservación total** de nodos `<Scan>` y `<Poi>` (beatgrids, cues, loops)
- 🧹 **Limpieza selectiva**: Elimina `Remix`, `TrackNumber`, `Bpm` duplicados

---

## 🚀 Inicio Rápido

### Requisitos
- **JDK 17** o superior
- **Maven 3.6+**
- (Opcional) **VirtualDJ** instalado para sincronización de base de datos

### Instalación

```bash
# 1. Clonar repositorio
git clone https://github.com/TU_USUARIO/audio-renamer.git
cd audio-renamer

# 2. Compilar
mvn clean install

# 3. Ejecutar
mvn javafx:run
