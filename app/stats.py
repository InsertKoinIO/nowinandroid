import re
from collections import defaultdict

def process_file(file_path):
    with open(file_path, 'r') as file:
        content = file.read()

    # Split the content into sessions using double newlines as delimiters
    sessions = content.strip().split("\n\n")

    # Data structure to store times across all sessions
    all_component_times = defaultdict(list)

    # Iterate over each session and process
    for session_index, session in enumerate(sessions):
        component_times = defaultdict(list)
        
        # Split the session into lines and parse each line
        lines = session.strip().split("\n")
        for line in lines:
            match = re.match(r"(.+?)=(.+)", line)
            if match:
                component = match.group(1)
                time = float(match.group(2))
                component_times[component].append(time)
                all_component_times[component].append(time)  # Collect times across all sessions
        
        # Calculate averages for each component in the current session
        print(f"Session {session_index + 1} Averages:")
        for component, times in component_times.items():
            average_time = sum(times) / len(times)
            print(f"{component}: {average_time:.3f} ms")
        print()  # Blank line for separating sessions

    # Calculate overall averages across all sessions
    print("Overall Averages Across All Sessions:")
    for component, times in all_component_times.items():
        overall_average = sum(times) / len(times)
        print(f"{component}: {overall_average:.3f} ms")

# Example usage
file_path = "benchmark_log.txt"  # Replace with the path to your file
process_file(file_path)
