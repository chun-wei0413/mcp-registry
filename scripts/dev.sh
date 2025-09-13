#!/bin/bash
# PostgreSQL MCP Server Development Environment Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
PROJECT_NAME="postgresql-mcp-server-dev"
COMPOSE_FILE="deployment/docker/docker-compose.dev.yml"
ENV_FILE=".env"

log_info() {
    echo -e "${BLUE}[DEV]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_dev_requirements() {
    log_info "Checking development requirements..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi

    # Check if ports are available
    if lsof -i :3000 &> /dev/null; then
        log_warning "Port 3000 is already in use"
    fi

    if lsof -i :5433 &> /dev/null; then
        log_warning "Port 5433 is already in use (dev database)"
    fi

    log_success "Requirements check completed"
}

setup_dev_environment() {
    log_info "Setting up development environment..."

    # Create .env for development if it doesn't exist
    if [ ! -f "$ENV_FILE" ]; then
        cp .env.example "$ENV_FILE"
        log_info "Created $ENV_FILE for development"
    fi

    # Create logs directory
    mkdir -p logs

    # Set development-specific environment variables
    export MCP_LOG_LEVEL=DEBUG
    export POSTGRES_MCP_ENCRYPTION_KEY="dev-encryption-key-not-for-production"

    log_success "Development environment setup completed"
}

start_dev_services() {
    log_info "Starting development services..."

    # Start all development services
    docker-compose -f "$COMPOSE_FILE" up -d

    log_success "Development services started"
}

wait_for_dev_services() {
    log_info "Waiting for development services..."

    local max_attempts=30
    local attempt=1

    # Wait for development database
    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f "$COMPOSE_FILE" exec -T postgres-dev pg_isready -U mcp_dev_user -d mcp_dev &>/dev/null; then
            log_success "Development database is ready"
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            log_error "Development database failed to start"
            exit 1
        fi

        sleep 2
        ((attempt++))
    done

    # Wait for test database
    attempt=1
    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f "$COMPOSE_FILE" exec -T postgres-test pg_isready -U mcp_test_user -d mcp_test &>/dev/null; then
            log_success "Test database is ready"
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            log_warning "Test database failed to start, but continuing..."
            break
        fi

        sleep 2
        ((attempt++))
    done

    log_success "All services are ready"
}

show_dev_status() {
    log_info "Development environment status:"
    docker-compose -f "$COMPOSE_FILE" ps

    echo ""
    log_info "Development URLs:"
    echo "  - MCP Server (dev): http://localhost:3000"
    echo "  - Dev Database: localhost:5433"
    echo "  - Test Database: localhost:5434"
    echo "  - Redis (dev): localhost:6379"

    echo ""
    log_info "Useful commands:"
    echo "  # View logs"
    echo "  docker-compose -f $COMPOSE_FILE logs -f [service]"
    echo ""
    echo "  # Run tests"
    echo "  python run_tests.py unit"
    echo "  python run_tests.py integration"
    echo ""
    echo "  # Connect to dev database"
    echo "  docker-compose -f $COMPOSE_FILE exec postgres-dev psql -U mcp_dev_user -d mcp_dev"
    echo ""
    echo "  # Stop services"
    echo "  docker-compose -f $COMPOSE_FILE down"
}

run_tests() {
    log_info "Running tests in development environment..."

    # Set test database URL
    export TEST_DATABASE_URL="postgresql://mcp_test_user:mcp_test_password@localhost:5434/mcp_test"

    case "${1:-all}" in
        "unit")
            python run_tests.py unit
            ;;
        "integration")
            python run_tests.py integration
            ;;
        "all")
            python run_tests.py all
            ;;
        "coverage")
            python run_tests.py coverage
            ;;
        *)
            log_error "Unknown test type: $1"
            echo "Available: unit, integration, all, coverage"
            exit 1
            ;;
    esac
}

watch_logs() {
    local service="${1:-mcp-server-dev}"
    log_info "Watching logs for $service..."
    docker-compose -f "$COMPOSE_FILE" logs -f "$service"
}

connect_db() {
    local db_type="${1:-dev}"
    case "$db_type" in
        "dev")
            log_info "Connecting to development database..."
            docker-compose -f "$COMPOSE_FILE" exec postgres-dev psql -U mcp_dev_user -d mcp_dev
            ;;
        "test")
            log_info "Connecting to test database..."
            docker-compose -f "$COMPOSE_FILE" exec postgres-test psql -U mcp_test_user -d mcp_test
            ;;
        *)
            log_error "Unknown database type: $db_type"
            echo "Available: dev, test"
            exit 1
            ;;
    esac
}

reset_databases() {
    log_info "Resetting databases..."

    # Stop services
    docker-compose -f "$COMPOSE_FILE" down

    # Remove volumes
    docker volume rm $(docker volume ls -q | grep $(basename $(pwd))) 2>/dev/null || true

    # Restart services
    docker-compose -f "$COMPOSE_FILE" up -d

    wait_for_dev_services

    log_success "Databases reset completed"
}

show_help() {
    echo "PostgreSQL MCP Server Development Environment"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  start      Start development environment (default)"
    echo "  stop       Stop all development services"
    echo "  restart    Restart development services"
    echo "  status     Show development environment status"
    echo "  logs       Show logs for service (default: mcp-server-dev)"
    echo "  test       Run tests [unit|integration|all|coverage]"
    echo "  db         Connect to database [dev|test] (default: dev)"
    echo "  reset      Reset databases and volumes"
    echo "  cleanup    Clean up development containers and volumes"
    echo "  help       Show this help"
    echo ""
    echo "Examples:"
    echo "  $0 start           # Start development environment"
    echo "  $0 logs postgres-dev # Show database logs"
    echo "  $0 test unit       # Run unit tests"
    echo "  $0 db test         # Connect to test database"
}

# Main function
start_dev() {
    log_info "Starting PostgreSQL MCP Server development environment"

    check_dev_requirements
    setup_dev_environment
    start_dev_services
    wait_for_dev_services
    show_dev_status

    log_success "Development environment is ready!"
    log_info "Run '$0 logs' to watch the server logs"
}

# Parse arguments
case "${1:-start}" in
    "start")
        start_dev
        ;;
    "stop")
        log_info "Stopping development services..."
        docker-compose -f "$COMPOSE_FILE" down
        log_success "Development services stopped"
        ;;
    "restart")
        log_info "Restarting development services..."
        docker-compose -f "$COMPOSE_FILE" restart
        show_dev_status
        ;;
    "status")
        show_dev_status
        ;;
    "logs")
        watch_logs "${2:-mcp-server-dev}"
        ;;
    "test")
        run_tests "${2:-all}"
        ;;
    "db")
        connect_db "${2:-dev}"
        ;;
    "reset")
        reset_databases
        ;;
    "cleanup")
        log_info "Cleaning up development environment..."
        docker-compose -f "$COMPOSE_FILE" down -v
        docker system prune -f
        log_success "Cleanup completed"
        ;;
    "help"|"--help"|"-h")
        show_help
        ;;
    *)
        log_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac