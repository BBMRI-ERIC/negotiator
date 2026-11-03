---
status: accepted
---

# Required Authority is a set

Each Transition carries a Required Authority, held in `transition.required_authority` and kept apart from the Guard chain. Asking who is firing and asking whether the domain permits the move behave differently. The Evaluation Pipeline checks Required Authority first, before the information-requirement check and the Guards.

Required Authority is a set of values, and a caller may fire the Transition if it holds any one of them. The values are:

- `IS_ADMIN`, a person whose token carries the granted authority `ROLE_ADMIN`. The `admin` column of the person row plays no part. The same value serves both Definition Scopes.
- `IS_CREATOR`, the Negotiation's creator.
- `IS_REPRESENTATIVE`, a representative of the related Resource.
- `SYSTEM`, the Orchestration Trigger.

A set is needed because some Negotiation Transitions can be fired by an admin or the creator. For example, `SUBMIT`, `CONCLUDE` and `ABANDON` have an authority set of `{IS_ADMIN, IS_CREATOR}`. `APPROVE` and `DECLINE` only have `{IS_ADMIN}`.

An empty set requires no authority, so any person may fire the Transition.

The Orchestration Trigger holds `SYSTEM` and no other value. An empty set does not admit it, or it could fire every Transition that requires no authority. A set that holds `SYSTEM` holds no other value, so either the system or people fire a Transition, never both. A CHECK on the column refuses an unknown value and `SYSTEM` beside another value. Definition Compilation checks the same rules again, because it reads rows and does not rely on the CHECK.

## Considered options

- A single value per Transition, with a blanket admin-or-creator check outside the Transition Evaluator. Transitions open to an admin or the creator would carry no value, and in Negotiation scope no value would mean "admin or creator". A definition author cannot see that restriction in the rows, and the evaluator would no longer be the only authority check.
- Combined values such as `IS_ADMIN_OR_IS_CREATOR`. Each new combination adds another value.
