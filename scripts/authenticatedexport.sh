#!/usr/bin/env bash

# Constants
SCRIPT_DIR="$(dirname "$BASH_SOURCE")"

# Mandatory variables
studio_hostname=
query_url=
user_var=
pass_var=

# Optional variables
target_dir="$SCRIPT_DIR"
filename=""
protocol="https"

# Authenticate and get cookie from Studio
authenticate () {
  status_code=$(curl -fks -w "%{http_code}" -c "$SCRIPT_DIR/cookie.txt" -d "j_username=$user_var&j_password=$pass_var" "$protocol://$studio_hostname/studio/login")
  handle_response "Authentication" "$protocol://$studio_hostname/studio/login" "$status_code"
}

# Use cookie to make authenticated export
export () {
  # If there was no user specified timestamp, create a default filename
  if [ "$filename" == "" ]; then
    filename="Report_$(date +"%F_%H-%M-%S")"
  fi
  if [ ${filename: -4} != ".csv" ]; then
    filename="$filename.csv"
  fi
  status_code=$(curl -fks -w "%{http_code}" -o "$SCRIPT_DIR/$filename" -b "$SCRIPT_DIR/cookie.txt" "$protocol://$studio_hostname/studio/$query_url")
  handle_response "Export" "$protocol://$studio_hostname/studio/$query_url" "$status_code"
  # Move exported file to target directory if the option has been specified
  if [ "$target_dir" != "$SCRIPT_DIR" ]; then
    mv ./"$SCRIPT_DIR/$filename" "$SCRIPT_DIR/$target_dir/"
  fi
}

# Check what response was received and clean cookies
handle_response () {
  # Non-200 response
  if [ "$3" != 200 ]; then
    echo "[$(date +"%F %T")] $1 request to $2 was unsuccessful, responded with $3." 1>&2
    if [ -f "$SCRIPT_DIR/cookie.txt" ]; then
      rm "$SCRIPT_DIR/cookie.txt"
    fi
    exit 1
  # Successful export response
  elif [ $1 == "Export" ]; then
    # Clear cookie file of successful export request
    rm "$SCRIPT_DIR/cookie.txt"
  fi
}

process_args () {
  # Iterate over all arguments and set variables
  while [ "$1" != "" ]; do
    case "$1" in
      -u | --user )            shift
                               user_var=$1
                               ;;
      -p | --password )        shift
                               pass_var=$1
                               ;;
      -sh | --studiohost )     shift
                               studio_hostname=$1
                               ;;
      -qu | --queryurl )       shift
                               query_url=$1
                               ;;
      -d | --directory )       shift
                               target_dir=$1
                               ;;
      -f | --filename )        shift
                               filename=$1
                               ;;
      -pr | --protocol )       shift
                               protocol=$1
                               ;;
    esac
    shift
  done
  # Check if all required arguments are set, else exit
  if [ "$studio_hostname" != "" -a "$query_url" != "" -a "$user_var" != "" ]; then
    if [ "$pass_var" != "" ]; then
      authenticate
      export
    else
      read -sp "Please enter a password: `echo $'\n> '`" pass_var
      authenticate
      export
    fi
  else
    echo "Insufficient arguments were provided." 1>&2
  fi
}

# Entry point
process_args "$@"
