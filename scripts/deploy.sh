#!/bin/bash
# PostgreSQL MCP Server Deployment Script

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="postgresql-mcp-server"
DOCKER_IMAGE="${PROJECT_NAME}:latest"
COMPOSE_FILE="deployment/docker/docker-compose.yml"
ENV_FILE=".env"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
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

check_requirements() {
    log_info "Checking requirements..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi

    log_success "All requirements satisfied"
}

setup_environment() {
    log_info "Setting up environment..."

    if [ ! -f "$ENV_FILE" ]; then
        if [ -f ".env.example" ]; then
            cp .env.example "$ENV_FILE"
            log_warning "Created $ENV_FILE from .env.example. Please review and update the configuration."
        else
            log_error ".env.example file not found. Cannot create environment file."
            exit 1
        fi
    fi

    # Generate encryption key if not set
    if ! grep -q "POSTGRES_MCP_ENCRYPTION_KEY=" "$ENV_FILE" || grep -q "your-encryption-key-change-this" "$ENV_FILE"; then
        ENCRYPTION_KEY=$(openssl rand -base64 32 2>/dev/null || head -c 32 /dev/urandom | base64)
        sed -i.bak "s/your-encryption-key-change-this/$ENCRYPTION_KEY/" "$ENV_FILE"
        log_info "Generated new encryption key"
    fi

    log_success "Environment setup completed"
}

build_image() {
    log_info "Building Docker image..."

    docker build -f deployment/docker/Dockerfile -t "$DOCKER_IMAGE" .

    log_success "Docker image built successfully"
}

deploy_services() {
    log_info "Deploying services..."

    # Pull latest images
    docker-compose -f "$COMPOSE_FILE" pull postgres pgadmin

    # Start services
    docker-compose -f "$COMPOSE_FILE" up -d

    log_success "Services deployed successfully"
}

wait_for_services() {
    log_info "Waiting for services to be ready..."

    # Wait for PostgreSQL
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U mcp_user -d mcp_test &>/dev/null; then
            log_success "PostgreSQL is ready"
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            log_error "PostgreSQL failed to start within timeout"
            exit 1
        fi

        log_info "Waiting for PostgreSQL... (attempt $attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done

    # Wait for MCP Server
    attempt=1
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s http://localhost:3000/health &>/dev/null; then
            log_success "MCP Server is ready"
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            log_warning "MCP Server health check failed, but continuing..."
            break
        fi

        log_info "Waiting for MCP Server... (attempt $attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done
}

show_status() {
    log_info "Service status:"
    docker-compose -f "$COMPOSE_FILE" ps

    log_info "Service URLs:"
    echo "  - MCP Server: http://localhost:3000"
    echo "  - PostgreSQL: localhost:5432"
    echo "  - PgAdmin: http://localhost:5050"
    echo "    - Email: admin@example.com"
    echo "    - Password: admin"

    log_info "To view logs:"
    echo "  docker-compose -f $COMPOSE_FILE logs -f [service-name]"

    log_info "To stop services:"
    echo "  docker-compose -f $COMPOSE_FILE down"
}

run_health_check() {
    log_info "Running health checks..."

    # Check MCP Server health
    if curl -f -s http://localhost:3000/health | jq '.status' 2>/dev/null; then
        log_success "MCP Server health check passed"
    else
        log_warning "MCP Server health check failed or jq not available"
    fi

    # Check database connection
    if docker-compose -f "$COMPOSE_FILE" exec -T postgres psql -U mcp_user -d mcp_test -c "SELECT 1;" &>/dev/null; then
        log_success "Database connection check passed"
    else
        log_error "Database connection check failed"
    fi
}

cleanup() {
    log_info "Cleaning up..."

    # Remove stopped containers
    docker-compose -f "$COMPOSE_FILE" rm -f

    # Clean up unused images
    docker image prune -f

    log_success "Cleanup completed"
}

show_help() {
    echo "PostgreSQL MCP Server Deployment Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  deploy     Deploy all services (default)"
    echo "  build      Build Docker image only"
    echo "  start      Start existing services"
    echo "  stop       Stop all services"
    echo "  restart    Restart all services"
    echo "  status     Show service status"
    echo "  logs       Show service logs"
    echo "  health     Run health checks"
    echo "  cleanup    Clean up stopped containers and unused images"
    echo "  help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 deploy          # Full deployment"
    echo "  $0 build           # Build image only"
    echo "  $0 logs mcp-server # Show MCP server logs"
}

# Main deployment function
deploy() {
    log_info "Starting deployment of $PROJECT_NAME"

    check_requirements
    setup_environment
    build_image
    deploy_services
    wait_for_services
    run_health_check
    show_status

    log_success "Deployment completed successfully!"
}

# Parse command line arguments
case "${1:-deploy}" in
    "deploy")
        deploy
        ;;
    "build")
        check_requirements
        build_image
        ;;
    "start")
        log_info "Starting services..."
        docker-compose -f "$COMPOSE_FILE" up -d
        show_status
        ;;
    "stop")
        log_info "Stopping services..."
        docker-compose -f "$COMPOSE_FILE" down
        log_success "Services stopped"
        ;;
    "restart")
        log_info "Restarting services..."
        docker-compose -f "$COMPOSE_FILE" restart
        show_status
        ;;
    "status")
        show_status
        ;;
    "logs")
        service_name="${2:-}"
        if [ -n "$service_name" ]; then
            docker-compose -f "$COMPOSE_FILE" logs -f "$service_name"
        else
            docker-compose -f "$COMPOSE_FILE" logs -f
        fi
        ;;
    "health")
        run_health_check
        ;;
    "cleanup")
        cleanup
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