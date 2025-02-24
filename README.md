# Temporal_Database
Temporal Database solution to enable temporal features in RDBMS


x = [1,2,3,5,10,15]
y_pt = [70.0, 81.6667, 81.6667, 90.0, 98.3333, 100.0]   #[70.0, 81.6667, 81.6667, 90.0]  [81.6667, 90.0, 98.3333, 100.0]
y_pt_gma =  [70.0, 81.6667, 86.6667, 96.6667, 98.3333, 100.0]  #[70.0, 81.6667, 86.6667, 96.6667] [86.6667, 96.6667, 98.3333, 100.0]
y_ceval = [70.0, 81.6667, 91.6667, 93.3333, 98.3333, 100.0] # 

# data_size 1000 , noise 0.8
[45.8333, 65.8333, 65.8333, 65.8333, 89.1667, 95.8333] [45.8333, 45.8333, 65.8333, 81.6667, 92.5, 92.5] [48.3333, 70.0, 85.0, 97.5, 97.5, 99.1667]


# Data Size 2000  noise 0.8
[41.25, 65.8333, 82.5, 94.5833, 99.5833, 99.5833] [41.25, 65.8333, 82.5, 89.1667, 98.3333, 99.5833] [44.5833, 69.5833, 82.9167, 94.5833, 98.75, 99.1667]

# Data Size 2000  noise 0.5
[66.25, 80.4167, 94.5833, 97.9167, 100.0, 100.0] [66.25, 80.4167, 94.5833, 99.1667, 100.0, 100.0] [66.25, 88.75, 94.5833, 97.9167, 100.0, 100.0]

# Data Size 3000  noise 0.5
[70.2778, 85.8333, 93.0556, 99.7222, 100.0, 100.0] [70.2778, 85.8333, 93.0556, 99.7222, 100.0, 100.0] [70.2778, 88.0556, 94.4444, 99.4444, 100.0, 100.0]

# Data Size 3000  noise 0.8
[45.0, 72.2222, 72.2222, 72.2222, 89.1667, 94.1667] [45.0, 72.2222, 72.2222, 72.2222, 82.7778, 96.9444] [45.0, 72.2222, 72.2222, 72.2222, 85.2778, 92.5]

# Data Size 5000  noise 0.8
[45.3333, 71.8333, 71.8333, 71.8333, 71.8333, 82.8333] [45.3333, 71.8333, 71.8333, 71.8333, 71.8333, 82.8333] [45.3333, 71.8333, 71.8333, 84.3333, 84.3333, 89.5]

# Data Size 8000  noise 0.8
[46.875, 72.7083, 72.7083, 72.7083, 72.7083, 91.0417] [46.875, 72.7083, 72.7083, 72.7083, 72.7083, 91.0417] [46.875, 72.7083, 72.7083, 84.1667, 84.1667, 89.7917]

# Data Size 350  noise 0.8
[47.619, 71.4286, 71.4286, 71.4286, 97.619, 97.619] [50.0, 73.8095, 73.8095, 83.3333, 97.619, 97.619] [47.619, 71.4286, 71.4286, 71.4286, 80.9524, 95.2381]


# Data Size 250  noise 0.8
[0.0, 40.0, 40.0, 60.0, 83.3333, 96.6667] [0.0, 40.0, 40.0, 60.0, 90.0, 96.6667] [20.0, 46.6667, 46.6667, 46.6667, 73.3333, 86.6667]

# Data Size 150  noise 0.8
[0.0, 0.0, 50.0, 50.0, 94.4444, 94.4444] [0.0, 50.0, 50.0, 50.0, 77.7778, 94.4444] [0.0, 33.3333, 33.3333, 61.1111, 88.8889, 94.4444]


# Data Size 50  noise 0.8
[0.0, 0.0, 50.0, 50.0, 50.0, 50.0] [0.0, 0.0, 0.0, 50.0, 100.0, 100.0] [0.0, 0.0, 0.0, 0.0, 33.3333, 50.0]


# Data Size 25  noise 0.8
[0.0, 0.0, 0.0, 0.0, 0.0, 66.6667] [0.0, 0.0, 0.0, 0.0, 0.0, 66.6667] [0.0, 0.0, 0.0, 0.0, 66.6667, 66.6667]



-----------------------------------------------------------
plot_chart_for_topK(x,  y_pt, y_pt_gma, y_ceval)

import matplotlib.pyplot as plt

def plot_chart_for_topK(x, kpi_arr1, kpi_arr2, kpi_arr3):
    # Sample data
    # Create a line chart
    plt.figure(figsize=(8, 6))
    # plt.plot(x, y, marker='o', linestyle='-')

    df = pd.DataFrame({
        'x_values': x,
        'y1_values': kpi_arr1,
        'y2_values': kpi_arr2,
        'y3_values': kpi_arr3
        }
    )

    # plt.plot(x, kpi_arr1, marker='o', linestyle='-')

    plt.plot(
        'x_values', 'y1_values', data=df,
        marker='o', # marker type
        markerfacecolor='blue', # color of marker
        markersize=12, # size of marker
        color='skyblue', # color of line
        linewidth=2, # change width of line
        label="path_traversal" # label for legend
    )

    plt.plot(
        'x_values', 'y2_values', data=df,
        marker='x', # no marker
        color='olive', # color of line
        linewidth=4, # change width of line
        linestyle='dashed',
        label="path_traversal_gma" # label for legend
    )

    plt.plot(
        'x_values', 'y3_values', data=df,
        marker='', # no marker
        color='darkred', # color of line
        linewidth=3, # change width of line
        linestyle='dotted', # change type of line
        label="cost_evaluation" # label for legend
    )

    # Add title and labels
    plt.title('KPI (%) w.r.t. topK RCA Nodes')
    plt.xlabel('X-axis Label')
    plt.ylabel('Y-axis Label')

    # Display grid
    # plt.grid(True)

    # show legend
    plt.legend()

    # show graph
    plt.show()

    # Show the plot
    plt.show()
