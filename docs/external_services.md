# External services

This document describes the external services that are used by the Negotiator.

## Molgenis

[Molgenis](https://www.molgenis.org/) is an open-source data platform for scientists and researchers. Several
instances of it such as the [BBMRI-ERIC Directory](https://directory.bbmri-eric.eu) use The Negotiator for submitting
and managing access requests. The communication works both ways
as the Negotiator has an interface to interact with the Molgenis API. The main purpose of it is to verify and to
retrieve resources/entities used in access requests. Implementation of the REST client can be found
at `MolgenisServiceImplementation.java`