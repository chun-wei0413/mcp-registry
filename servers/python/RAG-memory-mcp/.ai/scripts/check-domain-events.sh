#!/bin/bash

# Check Domain Events Compliance with Coding Standards
# This script ensures all domain events follow the correct patterns

echo "🔍 Checking Domain Events compliance..."
echo "=============================================="

ERRORS=0
WARNINGS=0

# Find all *Events.java files
EVENT_FILES=$(find src/main/java -name "*Events.java" 2>/dev/null)

if [ -z "$EVENT_FILES" ]; then
    echo "⚠️ No domain event files found (*Events.java)"
    exit 0
fi

for FILE in $EVENT_FILES; do
    echo ""
    echo "📄 Checking: $FILE"
    echo "-------------------------------------------"
    
    # 1. Check if extends InternalDomainEvent
    if grep -q "extends InternalDomainEvent" "$FILE"; then
        echo "✅ Extends InternalDomainEvent"
    else
        echo "❌ Does NOT extend InternalDomainEvent!"
        echo "   Required: sealed interface XxxEvents extends InternalDomainEvent"
        ERRORS=$((ERRORS + 1))
    fi
    
    # 2. Check if uses sealed interface
    if grep -q "sealed interface" "$FILE"; then
        echo "✅ Uses sealed interface"
    else
        echo "❌ Does NOT use sealed interface!"
        echo "   Required: public sealed interface XxxEvents"
        ERRORS=$((ERRORS + 1))
    fi
    
    # 3. Check for ConstructionEvent implementation
    if grep -q "InternalDomainEvent.ConstructionEvent" "$FILE"; then
        echo "✅ Has ConstructionEvent implementation"
    else
        echo "⚠️ No ConstructionEvent found"
        echo "   Created events should implement InternalDomainEvent.ConstructionEvent"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    # 4. Check for DestructionEvent implementation
    if grep -q "InternalDomainEvent.DestructionEvent" "$FILE"; then
        echo "✅ Has DestructionEvent implementation"
    else
        echo "ℹ️ No DestructionEvent found (optional)"
    fi
    
    # 5. Check for TypeMapper
    if grep -q "class TypeMapper" "$FILE"; then
        echo "✅ Has TypeMapper for serialization"
    else
        echo "❌ Missing TypeMapper!"
        echo "   Required: Internal TypeMapper class with DomainEventTypeMapper"
        ERRORS=$((ERRORS + 1))
    fi
    
    # 6. Check for source() method (新版 API)
    if grep -q "source()" "$FILE"; then
        echo "✅ Has source() method"
    else
        echo "❌ Missing source() method!"
        echo "   Required: @Override default String source() - 新版 API 回傳聚合 ID"
        ERRORS=$((ERRORS + 1))
    fi
    
    # 7. Check for required event fields
    if grep -q "Map<String, String> metadata" "$FILE"; then
        echo "✅ Has metadata field"
    else
        echo "❌ Missing metadata field!"
        echo "   Required: Map<String, String> metadata in events"
        ERRORS=$((ERRORS + 1))
    fi
    
    if grep -q "UUID id" "$FILE"; then
        echo "✅ Has UUID id field"
    else
        echo "❌ Missing UUID id field!"
        echo "   Required: UUID id (not eventId) in events"
        ERRORS=$((ERRORS + 1))
    fi
    
    if grep -q "Instant occurredOn" "$FILE"; then
        echo "✅ Has Instant occurredOn field"
    else
        echo "❌ Missing occurredOn field!"
        echo "   Required: Instant occurredOn in events"
        ERRORS=$((ERRORS + 1))
    fi
    
    # 8. Check for wrong patterns
    if grep -q "implements ConstructionEvent[^.]" "$FILE"; then
        echo "❌ Using custom ConstructionEvent interface!"
        echo "   Must use: InternalDomainEvent.ConstructionEvent"
        ERRORS=$((ERRORS + 1))
    fi
    
    if grep -q "implements DestructionEvent[^.]" "$FILE"; then
        echo "❌ Using custom DestructionEvent interface!"
        echo "   Must use: InternalDomainEvent.DestructionEvent"
        ERRORS=$((ERRORS + 1))
    fi
done

# Summary
echo ""
echo "=============================================="
echo "📊 Domain Events Compliance Check Summary"
echo "=============================================="

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo "✅ All domain events comply with coding standards!"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo "⚠️ Domain events check completed with $WARNINGS warning(s)"
    echo "   Consider addressing the warnings for better compliance"
    exit 0
else
    echo "❌ Domain events check failed with $ERRORS error(s) and $WARNINGS warning(s)"
    echo ""
    echo "📋 Required Fixes:"
    echo "1. All event interfaces must extend InternalDomainEvent"
    echo "2. Use sealed interface with permits clause"
    echo "3. Created events must implement InternalDomainEvent.ConstructionEvent"
    echo "4. Deleted events must implement InternalDomainEvent.DestructionEvent"
    echo "5. Include TypeMapper class for serialization"
    echo "6. All events must have: metadata, id, occurredOn fields"
    echo ""
    echo "📖 Reference Template:"
    echo "   .ai/tech-stacks/java-ca-ezddd-spring/examples/domain/ProductEvents.java"
    exit 1
fi