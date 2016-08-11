using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Data;

using Npgsql;

namespace ScanManager.Common
{
    class SqlUtilities
    {
        public static int ExcecuteNonQuery(String ConnectionString, String SqlCommand)
        { return ExcecuteNonQuery(ConnectionString, SqlCommand, null); }

        public static int ExcecuteNonQuery(String ConnectionString, String SqlCommand, Dictionary<String, Object> aParams)
        {
            int RowsAffected = -1;
            using (NpgsqlConnection con = new NpgsqlConnection(ConnectionString))
            {
                con.Open();
                using (NpgsqlCommand cmd = con.CreateCommand())
                {
                    cmd.CommandText = SqlCommand;
                    if ((aParams != null) && (aParams.Count > 0))
                    {
                        foreach (KeyValuePair<String, Object> kvp in aParams)
                        {
                            cmd.Parameters.AddWithValue(kvp.Key, kvp.Value);
                        }
                    }
                    RowsAffected = cmd.ExecuteNonQuery();
                }
                con.Close();
            }
            return RowsAffected;
        }

        public static DataTable GetData(String ConnectionString, String SqlCommand)
        {
            DataTable dt = new DataTable();
            using (NpgsqlConnection con = new NpgsqlConnection(ConnectionString))
            {
                con.Open();
                using (NpgsqlCommand cmd = con.CreateCommand())
                {
                    cmd.CommandText = SqlCommand;
                    NpgsqlDataAdapter da = new NpgsqlDataAdapter(cmd);
                    da.Fill(dt);
                }
                con.Close();
            }
            return dt;
        }

        public static Object ExecuteScalar(String ConnectionString, String SqlCommand)
        {
            Object Result = null;
            using (NpgsqlConnection con = new NpgsqlConnection(ConnectionString))
            {
                con.Open();
                using (NpgsqlCommand cmd = con.CreateCommand())
                {
                    cmd.CommandText = SqlCommand;
                    Result = cmd.ExecuteScalar();
                }
                con.Close();
            }
            return Result;
        }
    }
}
