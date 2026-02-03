# JNI Numerical Integration

Java aplikacija za numeričku integraciju koja koristi:

- višedretveno računanje
- SQLite bazu podataka
- JNI (C) za nativno ubrzano računanje
- Java fallback ako native kod nije dostupan

Metode integracije:
- Trapezna metoda
- Simpsonova metoda

---

## Tehnologije

- Java 17+
- Maven
- Swing (GUI)
- SQLite + Hibernate
- JNI (C)
- GCC / MinGW

---

## Preduvjeti

### Za sve:
- Instaliran JDK (17 ili noviji)
- Instaliran Maven

### Za JNI:
- **Linux:** GCC
- **Windows:** MinGW ili MSYS2 (gcc)

---

## Build Java dijela

U root direktoriju projekta:

<pre>mvn clean package</pre>

Time se generira JAR datoteka u folderu:
<pre>target/jni-integration-0.1.0-SNAPSHOT.jar</pre>

## Build native biblioteke (JNI)

### Linux(WSL/Ubuntu)
<pre>cd src/main/c

gcc -shared -fPIC -o libintegrator.so integrator.c \
 -I"$JAVA_HOME/include" \
 -I"$JAVA_HOME/include/linux"</pre>

### Windows (Command Prompt / PowerShell)
<pre>cd src\main\c

gcc -shared -o integrator.dll integrator.c ^
 -I"%JAVA_HOME%\include" ^
 -I"%JAVA_HOME%\include\win32"</pre>

## Pokretanje aplikacije
Iz root direktorija

### Linux
<pre>java -Djava.library.path=src/main/c \
 -jar target/jni-integration-0.1.0-SNAPSHOT.jar
</pre>

### Windows
<pre>java -Djava.library.path=src\main\c ^
 -jar target\jni-integration-0.1.0-SNAPSHOT.jar
</pre>

## Način rada

Ako je native biblioteka prisutna koristi se C implementacija (JNI), ako nije automatski se koristi Java implementacija. Aplikacija uvijek radi, bez obzira postoji li native kod. 

## Napomena

JNI biblioteka je platform dependent (Linux koristi .so, a Windows koristi .dll). Potrebno je ponovno kompajlirati C kod za svaku platformu.

---

## JAVA_HOME

Za korištenje JNI-ja mora biti postavljena varijabla okruženja `JAVA_HOME` koja pokazuje na instalaciju JDK-a.

### Provjera

#### Linux / WSL
<pre>echo $JAVA_HOME</pre>

#### Windows
<pre>echo %JAVA_HOME%</pre>

Ako se ništa ne ispiše, `JAVA_HOME` nije postavljen.

---

### Postavljanje

#### Linux / WSL (primjer)
<pre>export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64</pre>

Za trajno postavljanje dodati u `~/.bashrc`:
<pre>export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64</pre>

---

#### Windows

1. Control Panel → System → Advanced system settings
2. Environment Variables → New
3. Dodati:
    - Name: `JAVA_HOME`
    - Value (primjer):
      <pre>`C:\Program Files\Java\jdk-17`</pre>

Zatvoriti i ponovno otvoriti terminal.

---

### Provjera JDK-a

<pre>`javac -version`</pre>

Ako se ispiše verzija, JDK je ispravno instaliran.
