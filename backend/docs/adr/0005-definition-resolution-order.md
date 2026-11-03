---
status: accepted
---

# Definition Resolution order

A Resource belongs to zero or more Networks, each may point at different Definition Families. Definition Resolution decides which family a Lifecycle runs under when it starts.

A Negotiation resolves to the one family with an active Negotiation-scope version, and zero or several such families is a configuration error.

A Resource resolves in this order of precedence:

1. A family associated directly with the Resource.
2. The family named by one of the Resource's Networks.
3. The Global Default Family.

A Resource in Networks that name different families is a conflict. The write that would create it fails and names the Networks involved. A Resource with a direct association cannot conflict.

## Considered options

Ranking Networks by priority would avoid conflicts, but it picks a family silently and a Resource may run the wrong Lifecycle unnoticed. A rejected write is loud, and the Network owners fix it.
