package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.enums.InvoiceType;
import api.astro.whats_orders_manager.models.Cliente;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.services.ClienteService;
import api.astro.whats_orders_manager.services.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/clientes")
@Slf4j
public class ClienteController {
    @Autowired
    private ClienteService clienteService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listarClientes(Model model) {
        model.addAttribute("clientes", clienteService.findAll());
        model.addAttribute("cliente", new Cliente());
        return "clientes/clientes"; // Retorna la vista de listado de clientes
    }

    @GetMapping("/nuevo")
    public String nuevoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/form";
    }

    @ModelAttribute("tiposCliente")
    public InvoiceType[] getTiposCliente() {
        return InvoiceType.values();
    }

    @PostMapping("/guardar")
    public String guardarCliente(@ModelAttribute Cliente cliente) {
        log.info("Iniciando el proceso de guardar el cliente "+ cliente);
        Optional<Usuario> usuario = usuarioService.findByTelefono(cliente.getUsuario().getTelefono());

        if (usuario.isEmpty()) {
            var usuarioNuevo = new Usuario();

            usuarioNuevo.setNombre(cliente.getNombre());
            usuarioNuevo.setPassword("12345");
            usuarioNuevo.setRol("CLIENTE");
            usuarioNuevo.setTelefono(cliente.getUsuario().getTelefono());
            log.info("Usuario creado: " + usuarioNuevo);
            usuarioService.save(usuarioNuevo);

            var clienteNuevo = new Cliente();
            clienteNuevo.setNombre(cliente.getNombre());
            clienteNuevo.setUsuario(usuarioNuevo);
            clienteNuevo.setTipoCliente(cliente.getTipoCliente());
            clienteService.save(clienteNuevo);
        } else {
            usuario.get().setNombre(cliente.getNombre());
            usuarioService.save(usuario.get());

            var clienteNuevo = new Cliente();
            clienteNuevo.setNombre(cliente.getNombre());
            clienteNuevo.setUsuario(usuario.get());
            clienteService.save(clienteNuevo);
        }

        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editarCliente(@PathVariable Integer id, Model model) {
        model.addAttribute("cliente", clienteService.findById(id).orElse(new Cliente()));
        return "clientes/form";
    }

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Integer id) {
        Optional<Cliente> cliente = clienteService.findById(id);
        return cliente.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/eliminar/{id}")
    public String eliminarCliente(@PathVariable Integer id) {
        clienteService.deleteById(id);
        return "redirect:/clientes";
    }
}