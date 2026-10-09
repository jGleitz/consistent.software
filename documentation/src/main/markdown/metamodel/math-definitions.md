$\def\valuedomain{\mathbb{V}}$
$\def\numberdomain{\mathbb{Q}}$
$\def\stringdomain{\mathbb{S}}$
$\def\labeldomain{\mathcal{L}}$
$\def\labels{\labeldomain}$
$\def\label{\mathtt}$
$\def\modelparts{\mathcal{M}}$

<!-- Parameterized type constructors -->

$\def\bags#1{\mathcal{B}\!\left(#1\right)}$
$\def\lists#1{#1^*}$
$\def\records#1{\left[\labeldomain\xrightharpoonup{}_{\mathrm{fin}}#1\right]}$

<!-- Layer-specific domains (for exposition) -->

$\def\layer#1{\mathbb{V}_{#1}}$

<!-- Type system -->

$\def\types{\mathcal{T}}$
$\def\semantics#1{[\![#1]\!]}$

<!-- Type constructors -->

$\def\tnum{\mathtt{Num}}$
$\def\tint{\mathtt{Int}}$
$\def\tsingleton#1{\mathtt{T}(#1)}$
$\def\tbag#1{\mathtt{Bag}(#1)}$
$\def\tlist#1#2{\mathtt{List}(#1, #2)}$
$\def\tcons#1#2{\mathtt{Cons}(#1, #2)}$
$\def\trecord#1{\mathtt{Record}(#1)}$
$\def\tclosedrecord#1{\mathtt{ClosedRecord}(#1)}$
$\def\tinterval#1#2#3#4{[#1, #2]_{#3, #4}}$
$\def\tmult#1#2{\mathtt{Mult}(#1, #2)}$
$\def\tunion#1#2{#1 \cup #2}$
$\def\tintersection#1#2{#1 \cap #2}$
$\def\tsetminus#1#2{#1 \setminus #2}$
$\def\trec#1#2{\mu #1.\, #2}$
$\def\tmurec#1#2{\mu(#1).\,(#2)}$
