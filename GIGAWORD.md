Concretely Annotated English Gigaword v5
===
This document will describe how the English Gigaword v5 corpus was converted into Concrete `Communication` objects.

Consult the most up-to-date version of this document from the `master` branch. 

Requirements
---
* JDK `>= 1.8.x`
* Apache Maven `>= 3.0.4`
* *nix operating system
* `git`
* Github account with SSH access
* An unpacked English Gigaword v5 corpus, accessible from the machine where the code will be run

Results for the paper were generated with the following tools:
* `jdk1.8.0_31` (Oracle)
* `Apache Maven 3.2.5 (12a6b3acb947671f09b81f49094c53f426d8cea1)`
* `CentOS 6.6 Final`
* `git version 1.8.5.1.109.g3d252a9`

Give me something to copy/paste (TLDR)
---
The following commands should work in an `sh` like shell.

Replace the following with paths in your own filesystem.

### Replace with your own paths

```sh
# Replace below with the path to your Gigaword Corpus
#                               ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓
export ENGLISH_GIGAWORD_V5_PATH=/path/to/your/corpus/

# Replace with the path where you want to write intermediate results
#                              ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓
export REPAIRED_IDS_OUTPUT_DIR=/path/to/your/output/dir

# Replace with the path to your desired output directory
#                          ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓
export CONCRETE_OUTPUT_DIR=/path/to/your/output/dir
```

### Run the converter
```sh
export EGV5_DATA_DIR="$ENGLISH_GIGAWORD_V5_PATH/data"
export REPAIRED_IDS_FILE="$REPAIRED_IDS_OUTPUT_DIR/filename-ids.txt.gz"
git clone git@github.com:hltcoe/concrete-gigaword.git
cd concrete-gigaword
git checkout v4.2.1-concretely-annotated
mvn clean compile assembly:single
java -XX:+UseG1GC -cp target/concrete-gigaword-4.2.1-jar-with-dependencies.jar \
  edu.jhu.hlt.concrete.gigaword.expt.RepairDuplicateIDs \
  $EGV5_DATA_DIR \
  $REPAIRED_IDS_OUTPUT_DIR
java -XX:+UseG1GC -cp target/concrete-gigaword-4.2.1-jar-with-dependencies.jar \
  edu.jhu.hlt.concrete.gigaword.expt.ConvertGigawordDocuments \
  $REPAIRED_IDS_FILE \
  $CONCRETE_OUTPUT_DIR
```

FAQ
--
##### Is the flag `-XX:+UseG1GC` required?
No.

##### What is `RepairDuplicateIDs`?
Some of the documents in English Gigaword v5 have duplicate IDs. This program repairs them, then creates a simple mapping between file path and document ID.

##### I'm getting `OutOfMemory` errors / non-deterministic crashing
You might need to add more memory to the `java` invocations. Try adding the flag `-Xmx` to the invocation and a larger amount of memory, e.g., `java -Xmx25G -XX:+UseG1GC ...`