package modelo;

public class ReglasDelJuego {
    private static final int AccionesPorTurno = 3;
    private static final int MaxRondas = 8;
    private static final int CapacidadInventario = 2;
    private static final int ValorReliquiaEspecial = 4;

    public static int getAccionesPorTurno() {
        return AccionesPorTurno;
    }

    public static int getMaxRondas() {
        return MaxRondas;
    }

    public static int getCapacidadInventario() {
        return CapacidadInventario;
    }

    public static int getValorReliquiaEspecial() {
        return ValorReliquiaEspecial;
    }
}
