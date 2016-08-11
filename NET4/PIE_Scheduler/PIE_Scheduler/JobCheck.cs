using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Data;
using Npgsql;

namespace PIE_Scheduler
{
    class JobCheck
    {
        public JobCheck() { }

        public void connectTest()
        {
            String connString = "Server=dwsys-dbcast01;Port=2280;Database=postgres;User Id=automationtest;Password=fnmaPASS";
            using (IDbConnection con = new NpgsqlConnection(connString))
            {
                con.Open();
                con.Close();
            }
        }
    }
}
