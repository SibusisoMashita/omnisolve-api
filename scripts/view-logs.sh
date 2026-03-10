#!/bin/bash

# Script to view CloudWatch logs for the Omnisolve API

LOG_GROUP="/aws/elasticbeanstalk/prod-omnisolve-api/var/log/web.stdout.log"
REGION="us-east-1"

echo "=== Omnisolve API Log Viewer ==="
echo ""

# Function to show menu
show_menu() {
    echo "Select an option:"
    echo "  1) View latest logs (last 50 lines)"
    echo "  2) View logs from last 5 minutes"
    echo "  3) View logs from last 30 minutes"
    echo "  4) Search for errors"
    echo "  5) Search for document uploads"
    echo "  6) Search for status transitions"
    echo "  7) Tail logs (live stream)"
    echo "  8) Custom search"
    echo "  9) Exit"
    echo ""
}

# Function to view latest logs
view_latest() {
    echo "Fetching latest logs..."
    aws logs tail "${LOG_GROUP}" \
        --region "${REGION}" \
        --since 5m \
        --format short \
        | tail -50
}

# Function to view logs from time period
view_time_period() {
    local minutes=$1
    echo "Fetching logs from last ${minutes} minutes..."
    aws logs tail "${LOG_GROUP}" \
        --region "${REGION}" \
        --since ${minutes}m \
        --format short
}

# Function to search logs
search_logs() {
    local pattern=$1
    local minutes=${2:-30}
    echo "Searching for '${pattern}' in last ${minutes} minutes..."
    aws logs tail "${LOG_GROUP}" \
        --region "${REGION}" \
        --since ${minutes}m \
        --filter-pattern "${pattern}" \
        --format short
}

# Function to tail logs
tail_logs() {
    echo "Streaming logs (Ctrl+C to stop)..."
    aws logs tail "${LOG_GROUP}" \
        --region "${REGION}" \
        --follow \
        --format short
}

# Main loop
while true; do
    show_menu
    read -p "Enter choice [1-9]: " choice
    echo ""
    
    case $choice in
        1)
            view_latest
            ;;
        2)
            view_time_period 5
            ;;
        3)
            view_time_period 30
            ;;
        4)
            search_logs "ERROR" 60
            ;;
        5)
            search_logs "Upload request received" 30
            ;;
        6)
            search_logs "Transitioning document" 30
            ;;
        7)
            tail_logs
            ;;
        8)
            read -p "Enter search pattern: " pattern
            read -p "Minutes to search (default 30): " minutes
            minutes=${minutes:-30}
            search_logs "${pattern}" ${minutes}
            ;;
        9)
            echo "Goodbye!"
            exit 0
            ;;
        *)
            echo "Invalid choice. Please try again."
            ;;
    esac
    
    echo ""
    echo "---"
    echo ""
done
