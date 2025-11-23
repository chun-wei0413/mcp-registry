#!/bin/bash

echo "🔍 InMemory Profile Configuration Validation Script"
echo "=================================================="
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

success_count=0
total_checks=0

function check_passed() {
    echo -e "✅ ${GREEN}$1${NC}"
    ((success_count++))
}

function check_failed() {
    echo -e "❌ ${RED}$1${NC}"
}

function check_warning() {
    echo -e "⚠️  ${YELLOW}$1${NC}"
}

function increment_total() {
    ((total_checks++))
}

echo "📋 Checking InMemory Profile Configuration..."
echo

# Check 1: InMemoryInfrastructureConfig exists
increment_total
if [ -f "src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryInfrastructureConfig.java" ]; then
    check_passed "InMemoryInfrastructureConfig.java exists"
else
    check_failed "InMemoryInfrastructureConfig.java is missing"
fi

# Check 2: Correct Profile annotation
increment_total
if grep -q '@Profile.*{"inmemory", "test-inmemory"}' src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryInfrastructureConfig.java; then
    check_passed "Correct @Profile annotation for InMemory profiles"
else
    check_failed "Missing or incorrect @Profile annotation"
fi

# Check 3: ConditionalOnMissingBean(DataSource.class)
increment_total
if grep -q '@ConditionalOnMissingBean(DataSource.class)' src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryInfrastructureConfig.java; then
    check_passed "ConditionalOnMissingBean(DataSource.class) annotation present"
else
    check_failed "Missing ConditionalOnMissingBean(DataSource.class) annotation"
fi

# Check 4: application-inmemory.properties configuration
increment_total
if [ -f "src/main/resources/application-inmemory.properties" ]; then
    if grep -q "spring.jpa.enabled=false" src/main/resources/application-inmemory.properties; then
        check_passed "JPA disabled in application-inmemory.properties"
    else
        check_failed "JPA not properly disabled in application-inmemory.properties"
    fi
else
    check_failed "application-inmemory.properties is missing"
fi

# Check 5: DataSource autoconfiguration excluded
increment_total
if grep -q "DataSourceAutoConfiguration" src/main/resources/application-inmemory.properties; then
    check_passed "DataSource autoconfiguration excluded"
else
    check_failed "DataSource autoconfiguration not excluded"
fi

# Check 6: JPA autoconfiguration excluded
increment_total
if grep -q "HibernateJpaAutoConfiguration" src/main/resources/application-inmemory.properties; then
    check_passed "JPA autoconfiguration excluded"
else
    check_failed "JPA autoconfiguration not excluded"
fi

# Check 7: No database configuration in inmemory properties
increment_total
if grep -q "spring.datasource.url" src/main/resources/application-inmemory.properties; then
    check_warning "Found DataSource configuration in InMemory profile (should be removed)"
else
    check_passed "No DataSource configuration in InMemory profile"
fi

# Check 8: InMemoryConfiguration provides MessageBus
increment_total
if grep -q "public MessageBus<DomainEvent> messageBus" src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryConfiguration.java; then
    check_passed "MessageBus<DomainEvent> bean provided by InMemoryConfiguration"
else
    check_failed "MessageBus<DomainEvent> bean not found in InMemoryConfiguration"
fi

# Check 9: Repository bean configuration
increment_total
if grep -q "public Repository.*productRepository" src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryConfiguration.java; then
    check_passed "Product Repository bean configured"
else
    check_failed "Product Repository bean not configured"
fi

# Check 10: GenericInMemoryRepository used
increment_total
if grep -q "GenericInMemoryRepository" src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryConfiguration.java; then
    check_passed "GenericInMemoryRepository used for repositories"
else
    check_failed "GenericInMemoryRepository not used"
fi

echo
echo "📊 Validation Summary:"
echo "====================="
echo -e "Total checks: $total_checks"
echo -e "Passed: ${GREEN}$success_count${NC}"
echo -e "Failed: ${RED}$((total_checks - success_count))${NC}"

if [ $success_count -eq $total_checks ]; then
    echo -e "\n🎉 ${GREEN}All checks passed! InMemory Profile configuration is correct.${NC}"
    echo
    echo "🚀 You can now start the application with:"
    echo "   mvn spring-boot:run -Dspring.profiles.active=inmemory"
    echo
    echo "📋 Or run tests with:"
    echo "   SPRING_PROFILES_ACTIVE=test-inmemory mvn test -q"
else
    echo -e "\n🔧 ${YELLOW}Some checks failed. Please review the configuration.${NC}"
fi

echo
echo "🔍 Key Configuration Files:"
echo "- InMemoryInfrastructureConfig: src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryInfrastructureConfig.java"
echo "- InMemoryConfiguration: src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryConfiguration.java"  
echo "- InMemory Properties: src/main/resources/application-inmemory.properties"
echo

echo "🏗️ InMemory Profile Event Architecture:"
echo "Repository.save() → MyInMemoryMessageBroker (MessageBus) → Reactors"
echo