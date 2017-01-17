namespace TestApplication
{
    partial class Form1
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.button1 = new System.Windows.Forms.Button();
            this.btnPathCheck = new System.Windows.Forms.Button();
            this.statusStrip1 = new System.Windows.Forms.StatusStrip();
            this.lblStatus = new System.Windows.Forms.ToolStripStatusLabel();
            this.btnTestNew = new System.Windows.Forms.Button();
            this.btnTestQuery = new System.Windows.Forms.Button();
            this.statusStrip1.SuspendLayout();
            this.SuspendLayout();
            // 
            // button1
            // 
            this.button1.Enabled = false;
            this.button1.Location = new System.Drawing.Point(164, 12);
            this.button1.Name = "button1";
            this.button1.Size = new System.Drawing.Size(108, 23);
            this.button1.TabIndex = 0;
            this.button1.Text = "ScanManager";
            this.button1.UseVisualStyleBackColor = true;
            this.button1.Click += new System.EventHandler(this.button1_Click);
            // 
            // btnPathCheck
            // 
            this.btnPathCheck.Location = new System.Drawing.Point(164, 86);
            this.btnPathCheck.Name = "btnPathCheck";
            this.btnPathCheck.Size = new System.Drawing.Size(75, 23);
            this.btnPathCheck.TabIndex = 1;
            this.btnPathCheck.Text = "Test Path";
            this.btnPathCheck.UseVisualStyleBackColor = true;
            this.btnPathCheck.Click += new System.EventHandler(this.btnPathCheck_Click);
            // 
            // statusStrip1
            // 
            this.statusStrip1.Items.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.lblStatus});
            this.statusStrip1.Location = new System.Drawing.Point(0, 239);
            this.statusStrip1.Name = "statusStrip1";
            this.statusStrip1.Size = new System.Drawing.Size(284, 22);
            this.statusStrip1.TabIndex = 2;
            this.statusStrip1.Text = "statusStrip1";
            // 
            // lblStatus
            // 
            this.lblStatus.Name = "lblStatus";
            this.lblStatus.Size = new System.Drawing.Size(52, 17);
            this.lblStatus.Text = "lblStatus";
            // 
            // btnTestNew
            // 
            this.btnTestNew.Location = new System.Drawing.Point(12, 12);
            this.btnTestNew.Name = "btnTestNew";
            this.btnTestNew.Size = new System.Drawing.Size(143, 23);
            this.btnTestNew.TabIndex = 3;
            this.btnTestNew.Text = "New Method";
            this.btnTestNew.UseVisualStyleBackColor = true;
            this.btnTestNew.Click += new System.EventHandler(this.btnTestNew_Click);
            // 
            // btnTestQuery
            // 
            this.btnTestQuery.Location = new System.Drawing.Point(164, 115);
            this.btnTestQuery.Name = "btnTestQuery";
            this.btnTestQuery.Size = new System.Drawing.Size(75, 23);
            this.btnTestQuery.TabIndex = 4;
            this.btnTestQuery.Text = "Test Query";
            this.btnTestQuery.UseVisualStyleBackColor = true;
            this.btnTestQuery.Click += new System.EventHandler(this.btnTestQuery_Click);
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(284, 261);
            this.Controls.Add(this.btnTestQuery);
            this.Controls.Add(this.btnTestNew);
            this.Controls.Add(this.statusStrip1);
            this.Controls.Add(this.btnPathCheck);
            this.Controls.Add(this.button1);
            this.Name = "Form1";
            this.Text = "Test Application";
            this.statusStrip1.ResumeLayout(false);
            this.statusStrip1.PerformLayout();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Button button1;
        private System.Windows.Forms.Button btnPathCheck;
        private System.Windows.Forms.StatusStrip statusStrip1;
        private System.Windows.Forms.ToolStripStatusLabel lblStatus;
        private System.Windows.Forms.Button btnTestNew;
        private System.Windows.Forms.Button btnTestQuery;
    }
}

