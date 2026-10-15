CREATE SEQUENCE IF NOT EXISTS staff_sequences START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS staff_attendance_sequences START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS payroll_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS staff (
    staff_id NUMERIC PRIMARY KEY DEFAULT nextval('staff_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    nama TEXT NOT NULL,
    nik TEXT NOT NULL DEFAULT '',
    tempat_lahir TEXT NOT NULL DEFAULT '',
    tanggal_lahir DATE,
    tanggal_join DATE,
    tanggal_resign DATE,
    jabatan TEXT NOT NULL,
    gaji_pokok NUMERIC NOT NULL DEFAULT 0,
    uang_makan NUMERIC NOT NULL DEFAULT 0,
    insentif NUMERIC NOT NULL DEFAULT 0,
    no_hp TEXT NOT NULL DEFAULT '',
    no_hp_darurat TEXT NOT NULL DEFAULT '',
    jenis_kelamin TEXT NOT NULL,
    pendidikan_terakhir TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT staff_jabatan_check CHECK (
        jabatan IN (
            'KASIR',
            'PROBATION',
            'KARYAWAN_ENTRY',
            'KARYAWAN_JUNIOR',
            'KARYAWAN_SENIOR'
        )
    ),
    CONSTRAINT staff_jenis_kelamin_check CHECK (
        jenis_kelamin IN ('PRIA', 'WANITA')
    ),
    CONSTRAINT staff_pendidikan_check CHECK (
        pendidikan_terakhir IN ('SD', 'SMP', 'SMA', 'S1', 'TIDAK_SEKOLAH')
    )
);

CREATE INDEX IF NOT EXISTS idx_staff_client_deleted
    ON staff (client_id, deleted_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_client_nik
    ON staff (client_id, lower(nik))
    WHERE deleted_at IS NULL AND nik <> '';

CREATE TABLE IF NOT EXISTS staff_attendance (
    attendance_id NUMERIC PRIMARY KEY DEFAULT nextval('staff_attendance_sequences'::regclass),
    staff_id NUMERIC NOT NULL,
    status TEXT NOT NULL,
    keterangan TEXT,
    attendance_date DATE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT staff_attendance_status_check CHECK (
        status IN ('HADIR', 'IZIN', 'SAKIT', 'ALFA')
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_attendance_day
    ON staff_attendance (staff_id, attendance_date);

CREATE INDEX IF NOT EXISTS idx_staff_attendance_date
    ON staff_attendance (attendance_date);

CREATE TABLE IF NOT EXISTS payroll (
    payroll_id NUMERIC PRIMARY KEY DEFAULT nextval('payroll_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    staff_id NUMERIC NOT NULL,
    staff_name TEXT NOT NULL,
    payroll_month INTEGER NOT NULL,
    payroll_year INTEGER NOT NULL,
    days_in_month INTEGER NOT NULL,
    absence_count INTEGER NOT NULL,
    deducted_days INTEGER NOT NULL,
    daily_rate NUMERIC NOT NULL,
    gaji_pokok NUMERIC NOT NULL,
    uang_makan NUMERIC NOT NULL,
    insentif NUMERIC NOT NULL,
    potongan NUMERIC NOT NULL,
    gaji_pokok_dibayar NUMERIC NOT NULL,
    total_payroll NUMERIC NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_payroll_client_month
    ON payroll (client_id, payroll_year, payroll_month, deleted_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payroll_staff_month
    ON payroll (staff_id, payroll_year, payroll_month)
    WHERE deleted_at IS NULL;
