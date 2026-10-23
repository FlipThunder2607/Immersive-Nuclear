☢️ Project: RBMK Nuclear Control & Computing System (1.12.2)

Contexto: Sistema de automação, monitoramento e controle para reatores RBMK em estilo de computação analógica/digital dos anos 1960/70.

🛠️ Fase 1: Arquitetura de Hardware & Máquinas Analógicas

Foco na interface física, painéis de controle e mecanismos manuais/analógicos.

[ ] 1.1. Painel Principal de Telemetria e Alertas (Luzes e Indicadores)

[ ] Definir padrões visuais (Verde = Operacional / Vermelho = Alarme/Erro).

[ ] Mapeamento dos módulos de luzes para indicadores de pressão, temperatura, fluxo de refrigeração e radiação.

[ ] Mecanismo analógico de lâmpadas incandescentes / relés de aviso sonoro (buzzer/alarmes).

[ ] 1.2. Interfaces de Controle Manual (Botões e Alavancas)

[ ] Módulo de Hastes de Controle (AZ-5 / Scram de Emergência - botão de cobertura articulada).

[ ] Painel de Alavancas para Válvulas de Refrigeração e Bombas de Água Principais.

[ ] Chaves Seletoras para Troca de Modos (Manual, Automático, Standby).

[ ] 1.3. Unidade de Impressão e Relatórios (Periféricos)

[ ] Máquina de Escrita Telegráfica / Teletype (TTY) para log físico em papel.

[ ] Leitor e Perfurador de Fitas de Papel (Armazenamento de dados/programas retro).

[ ] Plotter de Gráficos Analógicos para acompanhamento de curva de potência e xenon.

⚙️ Fase 2: Mapeamento de Entradas e Saídas (Sinais I/O)

Integração dos sinais analógicos e redstone com o reator RBMK.

[ ] 2.1. Sensores de Leitura do Reator (Inputs)

[ ] Captura de nível de potência do núcleo ($MW_{th}$).

[ ] Sensores de temperatura por canal de combustível.

[ ] Sensores de fluxo de vapor / água primária.

[ ] Detectores de contaminação por Xenônio ($Xe^{135}$) e Radiação de fundo.

[ ] 2.2. Atuadores e Mecanismos (Outputs)

[ ] Pinos/Atuadores de inserção e extração das hastes de boro/grafite.

[ ] Controle das bombas elétricas de alimentação de água.

[ ] Válvulas de alívio de pressão de emergência.

💻 Fase 3: Linguagem de Programação & Sistema Operacional Retro

Desenvolvimento da lógica de automação e linguagem baseada em cartões/comandos.

[ ] 3.1. Especificação da Linguagem (Ex: "RBMK-ASM" ou "SOV-60")

[ ] Sintaxe básica (Comandos de leitora de fita, instruções condicionais IF ERROR).

[ ] Conjunto de instruções para controle de ciclos de leitura I/O.

[ ] Registradores analógicos de estado.

[ ] 3.2. Console de Comando e Interatividade (Prompt)

[ ] Terminal CRT de fósforo verde com CLI (Command Line Interface).

[ ] Comandos de diagnostico (DIAG, STATUS, SCRAM, PRINT_LOG).

[ ] Rotinas de interrupção em tempo real para tratar alertas vermelhos automaticamente.

📚 Fase 4: Documentação Técnica & Manuais de Operação

Documentação no estilo soviético dos anos 60.

[ ] 4.1. Manual de Operação do Reator (SOP)

[ ] Protocolo de inicialização do reator.

[ ] Guia de interpretação do painel de luzes e alarmes.

[ ] Procedimento operacional padrão de emergência (Protocolo AZ-5).

[ ] 4.2. Documentação da Linguagem de Programação

[ ] Tabela de opcodes e instruções da linguagem.

[ ] Exemplos de scripts de automação para balanceamento térmico.

[ ] 4.3. Diagramas de Conexão e Esquece de Fiação

[ ] Mapeamento dos barramentos de sinais analógicos para as máquinas.
