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
    for session in sessions:
        # Split the session into lines and parse each line
        lines = session.strip().split("\n")
        for line in lines:
            match = re.match(r"(.+?)=(.+)", line)
            if match:
                component = match.group(1)
                time = float(match.group(2))
                all_component_times[component].append(time)  # Collect times across all sessions

    # Calculate overall min, max, and average across all sessions
    print("Overall Statistics Across All Sessions:")
    for component, times in all_component_times.items():
        overall_average = sum(times) / len(times)
        overall_min = min(times)
        overall_max = max(times)
        print(f"{component}: Avg = {overall_average:.3f} ms, Min = {overall_min:.3f} ms, Max = {overall_max:.3f} ms")

# Example usage
file_path = "benchmark_log.txt"  # Replace with the path to your file
process_file(file_path)
