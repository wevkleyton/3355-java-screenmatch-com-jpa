package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;
import org.apache.logging.log4j.util.PropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    private List<DadosSerie> dadosSeries = new ArrayList();
    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();
    private Optional<Serie> serieBusca;

    public Principal (SerieRepository repositorio){
        this.repositorio = repositorio;
    }
    public void exibeMenu() {
        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                   ___________________________________
                   | 1 - Buscar séries               |
                   | 2 - Buscar episódios            |
                   | 3 - Listar Series Buscadas      |
                   | 4 - Buscar Serie Por Titulo     |
                   | 5 - Buscar Serie Por Ator       |
                   | 6 - Top 5 Serie                 |
                   | 7 - Buacar series por categoria |
                   | 8 - Filtrar Series              |
                   | 9 - Buscar Episodio por trecho  |
                   | 10- Top Episodios por Serie     |
                   | 11- Buscar Episodios Por Data   |
                   |                                 |
                   | 0 - Sair                        |
                   -----------------------------------              
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriePorCategoria();
                    break;
                case 8:
                    filtrarSeriePorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosDepoisDeUmaData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }



    private void buscarSeriesPorAtor() {
        System.out.println("Qual o nome para a busca? ");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliscao Apartir de qual valor? ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Series em que " + nomeAtor + " trabalhou: ");
        seriesEncontradas.forEach(s -> System.out.println(s.getTitulo() + " Avaliacao: " + s.getAvaliacao()));

    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha um Titulo");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()){
            System.out.println("Dados da serie: " + serieBusca.get());
        }else {
            System.out.println("Serie nao encontrada!");
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
//        dadosSeries.add(dados);
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha uma Série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()){
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();
            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);

            repositorio.save(serieEncontrada);
        }else {
            System.out.println("Série não encontrada!");
        }
//        DadosSerie dadosSerie = getDadosSerie();

    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }


    private void buscarTop5Series() {
        List<Serie> seriesTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach(s -> System.out.println(s.getTitulo() + " avaliacao: " + s.getAvaliacao()));
    }

    private void buscarSeriePorCategoria(){
        System.out.println("Deseja buscar series de que categoria/genero? ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Series da categoria " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void filtrarSeriePorTemporadaEAvaliacao(){
        System.out.println("Filtrar series ate quantas temporadas? ");
        var totalTemporada = leitura.nextInt();
        leitura.nextLine();
        System.out.println("Com Avaliacao a partir de que valor? ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
//        List<Serie> filtrarSeries = repositorio.findByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(totalTemporada, avaliacao);
        List<Serie> filtrarSeries = repositorio.seriesPorTemporadeEAvaliacao(totalTemporada, avaliacao);
        filtrarSeries.forEach(s -> System.out.println(s.getTitulo() + " - avaliacao: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho(){
        System.out.println("Qual o nome do Epsodio para a busca? ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(s -> System.out.printf("Serie: %s Temporada %s - Episodio %s - %s\n",
                s.getSerie().getTitulo(), s.getTemporada(), s.getNumeroEpisodio(), s.getTitulo()));
    }

    private void topEpisodiosPorSerie(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
          Serie serie = serieBusca.get();
          List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
          topEpisodios.forEach(e -> System.out.printf("Serie: %s - Tempoarada %s - Episodio %s - %s Avaliscao %s\n",
                  e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao() ));
        }
    }

    private void buscarEpisodiosDepoisDeUmaData(){
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
           System.out.println("Digite o ano limite de lancamento! ");
           var anoLancamento = leitura.nextInt();
           leitura.nextLine();
           List<Episodio> episodiosAno = repositorio.epsodiosPorSerieEAno(serie, anoLancamento);
           episodiosAno.forEach(System.out::println);
        }
    }


}