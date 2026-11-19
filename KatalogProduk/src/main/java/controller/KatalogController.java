package controller;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import data.ProdukRepository;
import model.Produk;

public class KatalogController {

    @FXML
    private TextField namaField;
    @FXML
    private TextField hargaField;
    @FXML
    private ComboBox<String> fotoBox;
    @FXML
    private Label pesanLabel;
    @FXML
    private Label jumlahLabel;
    @FXML
    private TilePane katalogPane;
    @FXML
    private Button simpanButton;
    @FXML
    private Button batalButton;
    private final ProdukRepository repository = new ProdukRepository();

    // null = mode tambah; berisi id = sedang mengedit produk tersebut
    private Long idDiedit = null;

    @FXML
    private void initialize() {
        fotoBox.getItems().add("roti.png");
        fotoBox.getItems().add("susu.png");
        fotoBox.getItems().add("kopi.png");
        fotoBox.setValue("roti.png");
        try {
            showData();
        } catch (SQLException e) {
            pesanLabel.setText("Gagal membaca database. Lihat Output.");
            e.printStackTrace();
        }
    }

    private void showData() throws SQLException {
        ArrayList<Produk> daftar = repository.getAll();
        katalogPane.getChildren().clear();
        for (Produk produk : daftar) {
            VBox kartu = createProductCard(produk);
            katalogPane.getChildren().add(kartu);
        }
        jumlahLabel.setText("Jumlah produk: " + daftar.size());
    }

    private VBox createProductCard(Produk produk) {
        ImageView fotoView = new ImageView();
        fotoView.setFitWidth(170);
        fotoView.setFitHeight(110);
        fotoView.setPreserveRatio(true);
        StackPane bingkai = new StackPane();
        bingkai.setPrefSize(180, 120);
        bingkai.setMinSize(180, 120);
        bingkai.setMaxSize(180, 120);

        URL lokasi = getClass().getResource("/image/" + produk.getFoto());
        if (lokasi != null) {
            Image gambar = new Image(lokasi.toExternalForm(),
                    170, 110, true, true);
            if (!gambar.isError()) {
                fotoView.setImage(gambar);
                bingkai.getChildren().add(fotoView);
            } else {
                bingkai.getChildren().add(new Label("Gambar tidak terbaca"));
            }
        } else {
            bingkai.getChildren().add(new Label("Gambar tidak tersedia"));
        }

        Label namaLabel = new Label(produk.getNama());
        namaLabel.setWrapText(true);
        namaLabel.getStyleClass().add("nama-produk");
        Label hargaLabel = new Label("Rp" + produk.getHarga());
        Label idLabel = new Label("ID: " + produk.getId());
        idLabel.getStyleClass().add("muted");

        VBox kartu = new VBox();
        kartu.setSpacing(8);
        kartu.setMinWidth(220);
        kartu.setPrefWidth(220);
        kartu.setMaxWidth(220);
        kartu.getStyleClass().add("kartu-produk");
        kartu.getChildren().add(bingkai);
        kartu.getChildren().add(namaLabel);
        kartu.getChildren().add(hargaLabel);
        kartu.getChildren().add(idLabel);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("button-edit");
        editButton.setOnAction(e -> startEdit(produk));
        Button hapusButton = new Button("Hapus");
        hapusButton.getStyleClass().add("button-hapus");
        hapusButton.setOnAction(e -> deleteProduct(produk));
        HBox aksi = new HBox(8, editButton, hapusButton);
        kartu.getChildren().add(aksi);

        if (idDiedit != null && idDiedit == produk.getId()) {
            kartu.getStyleClass().add("kartu-diedit");
        }
        return kartu;
    }

    @FXML
    private void saveProduct() {
        Produk produk;
        try {
            String nama = namaField.getText();
            long harga = Long.parseLong(hargaField.getText().trim());
            String foto = fotoBox.getValue();
            if (idDiedit == null) {
                produk = new Produk(nama, harga, foto);
            } else {
                produk = new Produk(idDiedit, nama, harga, foto);
            }
        } catch (NumberFormatException e) {
            pesanLabel.setText("Harga harus angka bulat, contoh 12000.");
            return;
        } catch (IllegalArgumentException e) {
            pesanLabel.setText(e.getMessage());
            return;
        }
        boolean modeEdit = (idDiedit != null);
        try {
            if (modeEdit) {
                if (!repository.update(produk)) {
                    pesanLabel.setText("Produk tidak ditemukan, mungkin sudah dihapus.");
                    resetForm();
                    showData();
                    return;
                }
            } else {
                repository.add(produk);
            }
        } catch (SQLException e) {
            pesanLabel.setText("Gagal menyimpan. Lihat Output.");
            e.printStackTrace();
            return;
        }
        resetForm();
        try {
            showData();
            pesanLabel.setText(modeEdit
                    ? "Produk berhasil diperbarui."
                    : "Produk berhasil disimpan.");
        } catch (SQLException e) {
            pesanLabel.setText("Data tersimpan, tetapi katalog gagal dimuat ulang.");
            e.printStackTrace();
        }
    }

    private void startEdit(Produk produk) {
        idDiedit = produk.getId();
        namaField.setText(produk.getNama());
        hargaField.setText(String.valueOf(produk.getHarga()));
        if (!fotoBox.getItems().contains(produk.getFoto())) {
            fotoBox.getItems().add(produk.getFoto());
        }
        fotoBox.setValue(produk.getFoto());
        simpanButton.setText("Simpan Perubahan");
        batalButton.setVisible(true);
        batalButton.setManaged(true);
        pesanLabel.setText("Mengedit produk ID " + produk.getId() + ".");
        refreshKatalog();
    }

    @FXML
    private void cancelEdit() {
        resetForm();
        pesanLabel.setText("Edit dibatalkan.");
        refreshKatalog();
    }

    private void deleteProduct(Produk produk) {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Hapus Produk");
        konfirmasi.setHeaderText("Hapus \"" + produk.getNama() + "\"?");
        konfirmasi.setContentText("Data yang dihapus tidak dapat dikembalikan.");
        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isEmpty() || jawaban.get() != ButtonType.OK) {
            return;
        }
        try {
            repository.delete(produk.getId());
        } catch (SQLException e) {
            pesanLabel.setText("Gagal menghapus. Lihat Output.");
            e.printStackTrace();
            return;
        }
        // Jika produk yang sedang diedit dihapus, form dikembalikan ke mode tambah.
        if (idDiedit != null && idDiedit == produk.getId()) {
            resetForm();
        }
        try {
            showData();
            pesanLabel.setText("Produk berhasil dihapus.");
        } catch (SQLException e) {
            pesanLabel.setText("Data terhapus, tetapi katalog gagal dimuat ulang.");
            e.printStackTrace();
        }
    }

    private void resetForm() {
        idDiedit = null;
        namaField.clear();
        hargaField.clear();
        fotoBox.setValue("roti.png");
        simpanButton.setText("Simpan");
        batalButton.setVisible(false);
        batalButton.setManaged(false);
    }

    private void refreshKatalog() {
        try {
            showData();
        } catch (SQLException e) {
            pesanLabel.setText("Katalog gagal dimuat ulang. Lihat Output.");
            e.printStackTrace();
        }
    }
}
