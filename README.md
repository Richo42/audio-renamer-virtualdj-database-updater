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
```

### 📦 Generar JAR Ejecutable
Si prefieres empaquetar la aplicación en un archivo independiente para ejecutarlo directamente sin depender de Maven:

```bash
# Compilar y empaquetar
mvn package

# Ejecutar el archivo ejecutable generado
java -jar target/audio-renamer-1.0.jar
```

---

## 💻 Uso

### Flujo Básico
1. Seleccionar carpeta de origen con archivos de audio desordenados
2. Seleccionar `database.xml` de VirtualDJ (ruta típica: `Documents/VirtualDJ/database.xml`)
3. Hacer clic en **"Iniciar Renombrado"**
4. Revisar ventanas modales (si aparecen) escuchando preview y confirmando nombres
5. Esperar finalización: Los archivos renombrados estarán en `_RENOMBRADO`
6. Abrir VirtualDJ: Los tags se actualizarán automáticamente

### Formato de Salida

**Entrada:**
```text
( 92 bpm ) dj pokra - hermanos silva - la princesita sueña (ver).mp3
```

**Salida:**
```text
92 LA PRINCESITA SUEÑA - HERMANOS SILVA [DJ POKRA].mp3
```

**Tags en VirtualDJ:**
```xml
<Tags Title="92 LA PRINCESITA SUEÑA" Author="92 HERMANOS SILVA" Flag="1" Key="Fm" />
```

---

## 🏗️ Arquitectura del Proyecto

```text
src/main/java/com/renamer//
├── MainApp.java                 # Punto de entrada JavaFX
├── controller/
│   ├── MainController.java      # Ventana principal
│   └── AmbiguousDialogController.java # Modal con reproductor
├── service/
│   └── FileRenameService.java   # Lógica de renombrado (Task)
└── util/
    ├── BPMParser.java           # Extracción de BPM
    ├── SmartNameParser.java     # Análisis heurístico Artista/Canción
    └── VirtualDJDatabaseUpdater.java # Actualización segura de XML

src/main/resources/
├── fxml/
│   ├── main_view.fxml           # UI principal
│   └── ambiguous_dialog.fxml    # UI modal de validación
└── styles/
    └── styles.css               # Estilos JavaFX

pom.xml                          # Dependencias Maven
```

---

## 🔧 Componentes Clave

### `SmartNameParser.java`

#### Fases de Análisis:

1. **Extracción de DJ/Editor** (antes de limpiar basura)
   - Busca en `[]` o `()` $\rightarrow$ `[DJ POKRA]`
   - Busca standalone $\rightarrow$ `dj pokra - ...`
   - Busca al final $\rightarrow$ `... - DJ Mix`
2. **Limpieza de Basura**
   - Elimina `@spam`, `$`, `VIRUS`, `#tags`, puntos residuales
   - Limpia artefactos huérfanos $\rightarrow$ `- ( o - [`
3. **Separación Artista/Canción**
   - **0 guiones**: Modal manual
   - **1 guion**: Heurísticas (tags, artistas conocidos, longitud)
   - **>1 guion**: Modal manual (ambiguo)

#### Heurísticas de Decisión:


| Regla | Peso | Descripción |
| :--- | :---: | :--- |
| Coincide con artista conocido | **+5** | Busca en lista de 40+ artistas |
| Contiene Ft/Feat/X/Vs | **+3** | Indicador de colaboración |
| Texto corto ($\le$ 4 palabras) | **+1** | Probable artista |
| Sin tags de versión | **+2** | Probable artista |
| Contiene (Remix/Edit/VIP) | **+3** | Probable canción |
| Texto largo ($\ge$ 5 palabras) | **+1** | Probable canción |


### `FileRenameService.java`

#### Características:
- **Ejecución en hilo background** (`javafx.concurrent.Task`)
- **Actualización en tiempo real** de progreso y mensajes
- **Pausa controlada** con `CountDownLatch` para modales
- **Manejo de cancelación** (`Cancel All`)
- **Aislamiento de extensiones** desde el inicio para evitar corrupción

#### Método `stripResidualExtensions()`

Elimina extensiones duplicadas o mal posicionadas en medio de la cadena de texto:

```java
// Elimina ".mp3", ".wav", etc. pegados en medio del nombre
"La Canción.mp3 - Artista" → "La Canción - Artista"
```

### `AmbiguousDialogController.java`

#### Reproductor de Audio:
- **Inicialización lazy**: Solo carga si hay archivo
- **Sincronización bidireccional**:
  - Reproducción $\rightarrow$ Mueve slider
  - Arrastre slider $\rightarrow$ Seek en audio
- **Limpieza de recursos**: `disposeAudio()` en `setOnHidden`
- **Volumen seguro**: 30% para evitar golpes de audio

#### Flujo de Validación:
1. Usuario abre modal
2. Audio carga automáticamente
3. Usuario escucha preview
4. Edita campos si es necesario
5. Click en "Aceptar" $\rightarrow$ Retorna `ParseResult`
6. Modal cierra $\rightarrow$ Service continúa

### `VirtualDJDatabaseUpdater.java`

#### Protocolo Anti-Corrupción:

1. ✅ `isVirtualDJRunning()` $\rightarrow$ Bloquea si está abierto
2. 💾 Crear backup: `database.xml.backup.TIMESTAMP`
3. 📄 Parsear XML preservando estructura
4. 🔍 Buscar coincidencias por `FilePath` exacto
5. 📝 Actualizar SOLO:
   - `FilePath` (nueva ruta)
   - `FileSize` (nuevo tamaño)
   - `Title` (BPM + CANCIÓN)
   - `Author` (BPM + ARTISTA)
   - `LastModified` (timestamp)
6. 🗑️ Eliminar: `Remix`, `TrackNumber`, `Bpm` (duplicados)
7. 🚫 NO TOCAR: `Key`, `Flag`, `Cover`, `<Scan>`, `<Poi>`
8. 💾 Guardar en `database.xml.tmp`
9. 🔬 Validar XML generado
10. 🔄 Reemplazar atómicamente `database.xml`
11. 🪄 Eliminar backup si todo salió bien
12. ❌ Restaurar backup si algo falla

---

## 🎯 Integración con VirtualDJ

### Tags que se Escriben


| Campo XML | Formato | Ejemplo |
| :--- | :--- | :--- |
| `Title` | BPM + CANCIÓN (MAYÚSCULAS) | 92 LA PRINCESITA SUEÑA |
| `Author` | BPM + ARTISTA (MAYÚSCULAS) | 92 HERMANOS SILVA |
| `Remix` | ❌ Eliminado | Se limpia para evitar duplicados |
| `TrackNumber` | ❌ Eliminado | Se limpia para evitar duplicados |
| `Bpm` | ❌ Eliminado | Ya está en Title/Author |

### ⚠️ Requisitos Críticos

1. VirtualDJ DEBE estar cerrado al ejecutar el renombrado
2. Seleccionar `database.xml` antes de iniciar
3. No interrumpir el proceso mientras se actualiza el XML
4. Mantener backup hasta confirmar que VirtualDJ abre sin errores

---

## 📊 Ejemplos de Transformación


| Nombre Original | Nombre Renombrado | Tags VirtualDJ |
| :--- | :--- | :--- |
| `( 92 bpm ) dj pokra - hermanos silva - la princesita sueña (ver).mp3` | **92 LA PRINCESITA SUEÑA - HERMANOS SILVA [DJ POKRA].mp3** | `Title`: "92 LA PRINCESITA SUEÑA"<br>`Author`: "92 HERMANOS SILVA" |
| `You Salsa - No Te Contaron Mal [ AntonyDJ 2019 $$$ ] 85.mp3` | **85 NO TE CONTARON MAL - YOU SALSA [ANTONYDJ 2019].mp3** | `Title`: "85 NO TE CONTARON MAL"<br>`Author`: "85 YOU SALSA" |
| `1 - Amarte Hasta La Muerte - Grupo 5 - Domingos De Fiestas.mp3` | *(Modal manual)* $\rightarrow$<br>**1 AMARTE HASTA LA MUERTE - GRUPO 5.mp3** | `Title`: "1 AMARTE HASTA LA MUERTE"<br>`Author`: "1 GRUPO 5" |
| `Shakira Feat. Maluma - Chantaje (DJ Panda) - 102.aif` | **102 CHANTAJE - SHAKIRA FEAT. MALUMA [DJ PANDA].aif** | `Title`: "102 CHANTAJE"<br>`Author`: "102 SHAKIRA FEAT. MALUMA" |
| `imagen_001.jpg` | `---- imagen_001 ----` | *(No se procesa)* |

---

## 🧪 Testing

```bash
# Ejecutar tests unitarios
mvn test

# Cobertura de código (si usas JaCoCo)
mvn jacoco:report
```

### Casos de prueba recomendados:
- Archivos con BPM al inicio/final
- Nombres con 0, 1, y >1 guiones
- Archivos con tags corruptos o duplicados
- Base de datos VirtualDJ vacía vs. poblada

---

## 🤝 Contribuciones

1. Fork el repositorio
2. Crea rama feature: `git checkout -b feature/nueva-heuristica`
3. Commit cambios: `git commit -m "feat: mejora detección de artistas conocidos"`
4. Push a la rama: `git push origin feature/nueva-heuristica`
5. Abre Pull Request

### Guías de código:

- ✅ **Idioma**: Código en inglés, comentarios en español
- ✅ **Convenciones**: `PascalCase` (clases), `camelCase` (métodos/variables), `UPPER_SNAKE_CASE` (constantes)
- ✅ **Tests**: JUnit 5 para parsers y validadores
- ✅ **Documentación**: JavaDoc en métodos públicos

---

## 📄 Licencia

Distribuido bajo licencia MIT.

---

## 💡 Tips de Uso

- **Procesa en lotes** de 50-100 archivos para bibliotecas grandes
- **Revisa el 8-10%** que requiere modal: El parser tiene ~90-92% de precisión automática
- **Mantén VirtualDJ cerrado** hasta que el programa muestre "¡Completado!"
- **Usa el preview de audio** en modales dudosos: Escuchar 5 segundos puede ahorrarte renombrados incorrectos
- **Backup manual**: Copia `database.xml` antes de procesar miles de canciones

---

## 🪩 Agradecimientos

- **JavaFX** por la UI robusta y multiplataforma
- **JAudioTagger** (`net.jthink`) por el soporte de metadatos
- **VirtualDJ** por su formato XML documentado
- **Comunidad Maven** por las dependencias gestionadas

---

## 📬 Contacto

¿Problemas o sugerencias? Abre un *Issue* en GitHub o contáctame en `[TU_EMAIL]`.

---

> **Nota:** Este proyecto es de código abierto. Si te ahorra tiempo organizando tu biblioteca de DJ, considera darle una ⭐ en GitHub.
