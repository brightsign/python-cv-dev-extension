# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records for the BrightSign Python CV Extension project. ADRs document significant architectural decisions, their context, alternatives considered, and rationale.

## ADR Format

Each ADR follows this structure:
- **Status**: Proposed, Accepted, Deprecated, Superseded
- **Context**: The situation that necessitated a decision
- **Decision**: What was decided
- **Alternatives Considered**: Other options evaluated
- **Consequences**: Positive and negative outcomes
- **References**: Related documents, issues, or discussions

## Current ADRs

| ADR | Title | Status | Date |
|-----|-------|---------|------|
| [ADR-001](adr-001-cross-architecture-wheel-installation.md) | Cross-Architecture Wheel Installation Strategy | Accepted | 2025-01-28 |
| [ADR-002](adr-002-three-environment-build-architecture.md) | Three-Environment Build Architecture | Accepted | 2025-01-28 |
| [ADR-003](adr-003-package-preinstall-vs-runtime-install.md) | Package Pre-Installation vs Runtime Installation | Accepted | 2025-01-28 |

## Decision Drivers

Key factors influencing architectural decisions:
- **Cross-compilation complexity** (x86_64 → ARM64)
- **BrightSign filesystem constraints** (read-only areas)
- **Build reliability and reproducibility**
- **Extension deployment models** (development vs production)
- **Maintenance and debugging complexity**