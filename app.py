import streamlit as st
import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegression
from sklearn.cluster import KMeans

st.title("ChurnGuard")
st.write("Welcome! This App Predicts churn, shows customer segmentation, and explore data interactively.")

df = pd.read_csv('telecomdata (1).csv')
df_copy = df.copy()

binary_cols = []
multi_cols = []
for col in df.columns:
    if col in {'customerID', 'Churn'}:
        continue
    unique_len = len(df[col].unique())
    if unique_len == 2:
        binary_cols.append(col)
    elif unique_len > 2 and col != 'customerID' and df[col].dtype == 'object':
        multi_cols.append(col)

for col in binary_cols:
    df[col] = df[col].astype('category').cat.codes

df_encoded = pd.get_dummies(df, columns=multi_cols)

X = df_encoded.drop(columns=['customerID', 'Churn'])
X = X.fillna(X.median())
y = df_encoded['Churn']
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
model = LogisticRegression(max_iter=1000)
model.fit(X_train, y_train)

df_copy['TotalCharges'] = pd.to_numeric(df_copy['TotalCharges'], errors='coerce')
df_copy['TotalCharges'] = df_copy['TotalCharges'].fillna(df_copy['TotalCharges'].median())
features = df_copy[['tenure', 'MonthlyCharges', 'TotalCharges']]
Kmeans = KMeans(n_clusters=3, random_state=42)
Kmeans.fit(features)
labels = Kmeans.labels_
df_copy['Cluster'] = labels
cluster_names = {1: 'Loyal', 2: 'High-Risk', 0: 'At-Risk'}
df_copy['ClusterName'] = df_copy['Cluster'].map(cluster_names)

proba_churn = model.predict_proba(X_test)[:, 1]
df_copy.loc[X_test.index, 'Churn_Probability'] = proba_churn * 100
df_copy['Churn_Probability'] = df_copy['Churn_Probability'].round(2)

st.sidebar.title("Activity Selection")
option = st.sidebar.radio("Go to:", ["Dataset Overview", "EDA Plots", "Churn Prediction", "Customer Segmentation"])

if option == "Dataset Overview":
    st.subheader("Dataset Preview")
    st.dataframe(df_copy)
    st.write("Shape of dataset:", df_copy.shape)
    st.subheader("Summary Statistics")
    st.write(df_copy.describe())

elif option == "EDA Plots":
    st.subheader("Exploratory Data Analysis")
    all_cols = binary_cols + multi_cols
    selected_col = st.selectbox("Select a column to visualize churn:", all_cols)

    plt.figure(figsize=(8,5))
    raw_plot = sns.countplot(x=selected_col, hue='Churn', data=df)
    raw_plot.legend(title='Churn', labels=['No', 'Yes'])
    plt.title(f"Churn counts by {selected_col}")
    plt.grid(axis='y', linestyle='--')
    xticks_labels = df_copy[selected_col].unique()
    if(selected_col == 'SeniorCitizen'):
        raw_plot.set_xticklabels(['Female','male'])
    else:
        raw_plot.set_xticklabels(xticks_labels)
    st.pyplot(plt)

    if selected_col in binary_cols:
        total_1 = (df[selected_col]==1).sum()
        total_0 = (df[selected_col]==0).sum()
        churn_1 = ((df[selected_col]==1) & (df['Churn']==1)).sum()
        churn_0 = ((df[selected_col]==0) & (df['Churn']==1)).sum()
        churn_1_percnt = 100 * churn_1 / total_1
        churn_0_percnt = 100 * churn_0 / total_0
        no_churn_1 = 100 - churn_1_percnt
        no_churn_0 = 100 - churn_0_percnt
        data = pd.DataFrame({
            selected_col: [1, 1, 0, 0],
            'Churn': ['No', 'Yes', 'No', 'Yes'],
            'percent': [no_churn_1, churn_1_percnt, no_churn_0, churn_0_percnt]
        })
        plt.figure(figsize=(8,5))
        plot = sns.barplot(x=selected_col, y='percent', hue='Churn', data=data)
        plt.title(f"Churn Percentage by {selected_col}")
        plt.ylabel("Churn Percentage (%)")
        plt.grid(axis='y', linestyle='--')
        xticks_labels = df_copy[selected_col].unique()
        if(selected_col == 'SeniorCitizen'):
            plot.set_xticklabels(['Female','male'])
        else:
            plot.set_xticklabels(xticks_labels)
        st.pyplot(plt)

    else:
        summary = []
        for category in df[selected_col].unique():
            filtered_data = df[df[selected_col] == category]
            total_customers = len(filtered_data)
            churn_count = (filtered_data['Churn']==1).sum()
            non_churn_count = (filtered_data['Churn']==0).sum()
            summary.append({'Category': category, 'Churn': 100*churn_count/total_customers, 
                            'Non-Churn': 100*non_churn_count/total_customers})
        summary_df = pd.DataFrame(summary)
        summary_melted = summary_df.melt(id_vars='Category', value_vars=['Churn','Non-Churn'], 
                                         var_name='Churn Status', value_name='Percentage')
        plt.figure(figsize=(10,6))
        sns.barplot(x='Category', y='Percentage', hue='Churn Status', data=summary_melted)
        plt.title(f"Churn vs Non-Churn Percentage for {selected_col}")
        plt.ylabel("Percentage (%)")
        plt.grid(axis='y', linestyle='--')
        st.pyplot(plt)

elif option == "Churn Prediction":
    st.subheader("Predict Churn Probability for a Customer")
    cust_id = st.text_input("Enter CustomerID:")
    if cust_id:
        if cust_id in df_copy['customerID'].values:
            cust_row = df_copy[df_copy['customerID'] == cust_id]
            prob = cust_row['Churn_Probability'].values[0]
            if np.isnan(prob):
                st.write(f"Churn probability for Customer {cust_id} is not available.")
            else:
                st.write(f"Customer {cust_id} has a {prob}% probability of churn.")
        else:
            st.warning("CustomerID not found!")

elif option == "Customer Segmentation":
    st.subheader("Customer Segmentation")
    cust_id = st.text_input("Enter CustomerID to check cluster:")
    if cust_id:
        if cust_id in df_copy['customerID'].values:
            cust_row = df_copy[df_copy['customerID'] == cust_id]
            cluster = cust_row['ClusterName'].values[0]
            st.write(f"Customer {cust_id} belongs to cluster: {cluster}")
        else:
            st.warning("CustomerID not found!")
