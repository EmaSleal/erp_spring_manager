package api.astro.whats_orders_manager.modules.cliente.service.impl;

import api.astro.whats_orders_manager.modules.cliente.dto.ClienteConProblemasFe;
import api.astro.whats_orders_manager.modules.cliente.exception.DatosClienteInvalidosException;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.ProblemaDatoFe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final DatosFeValidator datosFeValidator;

    @Override
    public List<Cliente> findAll() {
        return clienteRepository.findAll();
    }

    @Override
    public Page<Cliente> findAll(Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    @Override
    public Optional<Cliente> findById(Integer id) {
        return clienteRepository.findById(id);
    }

    @Override
    public Cliente save(Cliente cliente) {
        if (requiereFe(cliente)) {
            List<ProblemaDatoFe> problemas = datosFeValidator.validarReceptor(cliente);
            if (!problemas.isEmpty()) {
                log.warn("Cliente {} no guardado: {} dato(s) FE inválido(s)", cliente.getIdCliente(), problemas.size());
                throw new DatosClienteInvalidosException(problemas);
            }
        }
        return clienteRepository.save(cliente);
    }

    @Override
    public void deleteById(Integer id) {
        clienteRepository.deleteById(id);
    }

    @Override
    public long count() {
        return clienteRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteConProblemasFe> clientesConDatosFeInvalidos() {
        // Evaluación en memoria con el validador compartido: pocos clientes y las reglas no se expresan en SQL.
        return clienteRepository.findAll().stream()
            .filter(ClienteServiceImpl::requiereFe)
            .sorted(Comparator.comparing(Cliente::getIdCliente, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(c -> new ClienteConProblemasFe(c, datosFeValidator.validarReceptor(c)))
            .filter(r -> !r.problemas().isEmpty())
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarClientesConDatosFeInvalidos() {
        return clientesConDatosFeInvalidos().size();
    }

    /** FE activa: solo un FALSE explícito la desactiva (igual que la emisión); null cuenta como activa. */
    private static boolean requiereFe(Cliente cliente) {
        return !Boolean.FALSE.equals(cliente.getRequiereFacturaElectronica());
    }
}
