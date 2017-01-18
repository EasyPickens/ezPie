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
                NpgsqlTransaction trans = con.BeginTransaction();
                try
                {
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
                    trans.Commit();
                }
                catch (Exception ex)
                {
                    // Attempt to roll back the transaction.
                    String messageRollback = ex.Message;
                    try
                    { trans.Rollback(); }
                    catch (Exception exRollback)
                    { messageRollback = String.Format("  Rollback Exception Type {0}: {1}", exRollback.GetType(), exRollback.Message); }
                    throw new Exception("Rolling back SQL transaction (ExecuteNonQuery).  " + messageRollback, ex);
                }
                con.Close();
            }
            return RowsAffected;
        }

        public static DataTable GetData(String ConnectionString, String SqlCommand)
        {
            return GetData(ConnectionString, SqlCommand, null);
        }

        public static DataTable GetData(String ConnectionString, String SqlCommand, Dictionary<String, Object> aParams)
        {
            DataTable dt = new DataTable();
            using (NpgsqlConnection con = new NpgsqlConnection(ConnectionString))
            {
                con.Open();
                NpgsqlTransaction trans = con.BeginTransaction();
                try
                {
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
                        NpgsqlDataAdapter da = new NpgsqlDataAdapter(cmd);
                        da.Fill(dt);
                    }
                    trans.Commit();
                }
                catch (Exception ex)
                {
                    String messageRollback = "";
                    try
                    { trans.Rollback(); }
                    catch (Exception exRollback)
                    { messageRollback = String.Format("  Rollback Exception Type {0}: {1}", exRollback.GetType(), exRollback.Message); }
                    throw new Exception("Rolling back SQL transaction (GetData)."+messageRollback, ex);
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
                NpgsqlTransaction trans = con.BeginTransaction();
                try
                {
                    using (NpgsqlCommand cmd = con.CreateCommand())
                    {
                        cmd.CommandText = SqlCommand;
                        Result = cmd.ExecuteScalar();
                    }
                    trans.Commit();
                }
                catch (Exception ex)
                {
                    String messageRollback = "";
                    try
                    { trans.Rollback(); }
                    catch (Exception exRollback)
                    { messageRollback = String.Format("  Rollback Exception Type {0}: {1}", exRollback.GetType(), exRollback.Message); }
                    throw new Exception("Rolling back SQL transaction (ExecuteScalar)."+messageRollback, ex);
                }
                con.Close();
            }
            return Result;
        }
    }
}
