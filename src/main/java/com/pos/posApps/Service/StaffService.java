package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.StaffDTO;
import com.pos.posApps.DTO.Dtos.StaffWriteRequest;
import com.pos.posApps.DTO.Enum.Jabatan;
import com.pos.posApps.DTO.Enum.JenisKelamin;
import com.pos.posApps.DTO.Enum.Pendidikan;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class StaffService {
    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    public List<StaffEntity> list(Long clientId) {
        return staffRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNull(clientId).stream()
                .sorted(Comparator
                        .comparing((StaffEntity staff) -> staff.getTanggalResign() != null)
                        .thenComparing(staff -> text(staff.getNama()), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public StaffDTO get(Long clientId, Long staffId) {
        return toDto(require(clientId, staffId));
    }

    @Transactional
    public void create(ClientEntity client, StaffWriteRequest request) {
        StaffEntity staff = new StaffEntity();
        staff.setClientEntity(client);
        apply(staff, request, null);
        staffRepository.save(staff);
    }

    @Transactional
    public void update(Long clientId, Long staffId, StaffWriteRequest request) {
        StaffEntity staff = require(clientId, staffId);
        apply(staff, request, staffId);
        staffRepository.save(staff);
    }

    public StaffDTO toDto(StaffEntity staff) {
        return new StaffDTO(
                staff.getStaffId(),
                staff.getNama(),
                staff.getNik(),
                staff.getTempatLahir(),
                staff.getTanggalLahir(),
                staff.getTanggalJoin(),
                staff.getTanggalResign(),
                staff.getJabatan() == null ? "" : staff.getJabatan().name(),
                moneyOrZero(staff.getGajiPokok()),
                moneyOrZero(staff.getUangMakan()),
                moneyOrZero(staff.getInsentif()),
                staff.getNoHp(),
                staff.getNoHpDarurat(),
                staff.getJenisKelamin() == null ? "" : staff.getJenisKelamin().name(),
                staff.getPendidikanTerakhir() == null ? "" : staff.getPendidikanTerakhir().name()
        );
    }

    public BigDecimal componentTotal(StaffEntity staff) {
        if (staff.getTanggalResign() != null) {
            return BigDecimal.ZERO;
        }
        return moneyOrZero(staff.getGajiPokok())
                .add(moneyOrZero(staff.getUangMakan()))
                .add(moneyOrZero(staff.getInsentif()));
    }

    private void apply(StaffEntity staff, StaffWriteRequest request, Long currentId) {
        if (request == null) {
            throw new StaffRuleException("Data karyawan wajib diisi");
        }

        String nama = requiredText(request.getNama(), "Nama", 120);
        String nik = optionalText(request.getNik(), 32);
        String tempatLahir = optionalText(request.getTempatLahir(), 80);
        if (request.getTanggalLahir() == null) {
            throw new StaffRuleException("Tanggal lahir wajib diisi");
        }
        if (request.getTanggalJoin() == null) {
            throw new StaffRuleException("Tanggal join wajib diisi");
        }
        if (request.getTanggalResign() != null && request.getTanggalResign().isBefore(request.getTanggalJoin())) {
            throw new StaffRuleException("Tanggal resign tidak boleh sebelum tanggal join");
        }
        if (!nik.isEmpty()) {
            Long clientId = staff.getClientEntity().getClientId();
            boolean duplicate = currentId == null
                    ? staffRepository.findByClientEntity_ClientIdAndNikIgnoreCaseAndDeletedAtIsNull(clientId, nik).isPresent()
                    : staffRepository.existsByClientEntity_ClientIdAndNikIgnoreCaseAndDeletedAtIsNullAndStaffIdNot(
                            clientId,
                            nik,
                            currentId
                    );
            if (duplicate) {
                throw new StaffRuleException("NIK sudah dipakai karyawan lain");
            }
        }

        staff.setNama(nama);
        staff.setNik(nik);
        staff.setTempatLahir(tempatLahir);
        staff.setTanggalLahir(request.getTanggalLahir());
        staff.setTanggalJoin(request.getTanggalJoin());
        staff.setTanggalResign(request.getTanggalResign());
        staff.setJabatan(enumValue(request.getJabatan(), Jabatan.class, "Jabatan"));
        staff.setGajiPokok(money(request.getGajiPokok(), "Gaji pokok"));
        staff.setUangMakan(money(request.getUangMakan(), "Uang makan"));
        staff.setInsentif(money(request.getInsentif(), "Insentif"));
        staff.setNoHp(optionalText(request.getNoHp(), 20));
        staff.setNoHpDarurat(optionalText(request.getNoHpDarurat(), 20));
        staff.setJenisKelamin(enumValue(request.getJenisKelamin(), JenisKelamin.class, "Jenis kelamin"));
        staff.setPendidikanTerakhir(enumValue(request.getPendidikanTerakhir(), Pendidikan.class, "Pendidikan terakhir"));
    }

    private StaffEntity require(Long clientId, Long staffId) {
        if (staffId == null) {
            throw new StaffRuleException("Karyawan tidak ditemukan", true);
        }
        return staffRepository.findByStaffIdAndDeletedAtIsNullAndClientEntity_ClientId(staffId, clientId)
                .orElseThrow(() -> new StaffRuleException("Karyawan tidak ditemukan", true));
    }

    private static String requiredText(String value, String label, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new StaffRuleException(label + " wajib diisi");
        }
        if (text.length() > max) {
            throw new StaffRuleException(label + " maksimal " + max + " karakter");
        }
        return text;
    }

    private static String optionalText(String value, int max) {
        String text = value == null ? "" : value.trim();
        if (text.length() > max) {
            throw new StaffRuleException("Teks maksimal " + max + " karakter");
        }
        return text;
    }

    private static BigDecimal money(BigDecimal value, String label) {
        if (value == null) {
            throw new StaffRuleException(label + " wajib diisi");
        }
        if (value.signum() < 0) {
            throw new StaffRuleException(label + " tidak boleh negatif");
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal moneyOrZero(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static <T extends Enum<T>> T enumValue(String value, Class<T> type, String label) {
        if (value == null || value.isBlank()) {
            throw new StaffRuleException(label + " wajib diisi");
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException exception) {
            throw new StaffRuleException(label + " tidak valid");
        }
    }
}
