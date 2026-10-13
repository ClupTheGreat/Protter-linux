PROTTER MODIFICATIONS
=====================

Based on the upstream ulo/Protter source tree.

Files:
  BatchProcess.java
  ProtProvider.java
  Prot.java.patch
  protter.config.patch

Changes:
1. Batch limit is controlled by maxBatchSize and should be set to 600.
2. Each protein is independently caught. A failure is logged as:
     SKIPPED <identifier>: <reason>
   and processing continues with the remaining proteins.
3. batchOutputDir optionally preserves every successful individual result and
   also copies the combined ZIP/PDF there. This is intended to be a Docker
   bind-mounted directory.
4. Batch filenames include the UniProt identifier/title and the UniProt
   recommended protein name when available.
5. Prot.java.patch additionally makes the UniProt recommended protein name
   appear as the plot title automatically when no explicit title is supplied.

IMPORTANT:
- The supplied BatchProcess.java and ProtProvider.java are complete drop-in
  replacements for the corresponding upstream master files.
- Prot.java is large, so it is supplied as a small exact patch rather than a
  reconstructed 761-line file. Apply it with `patch -p0 < Prot.java.patch`.
- protter.config.patch is a snippet: replace the existing `maxBatchSize = 100`
  line and add `batchOutputDir = /protter-output` to the real config.
