# CORS Setup

## Quick Fix
Create `src/main/java/{package}/config/CorsConfig.java`:
- AllowedOrigins: `http://localhost:5173`
- ExposedHeaders: `Location`, `Operation-Id`, `traceId`

See: `.ai/prompts/shared/common-rules.md` for full template

## Status
✅ Implemented in this project
